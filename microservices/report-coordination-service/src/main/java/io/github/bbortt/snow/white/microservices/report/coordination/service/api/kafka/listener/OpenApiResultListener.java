/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka.listener;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.CONSUMER_GROUP_ID;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.DEFAULT_CONSUMER_GROUP_ID;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.OPENAPI_CALCULATION_RESPONSE_TOPIC;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.ApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.OpenApiReportCalculator;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class OpenApiResultListener {

  private final ApiTestResultMapper apiTestResultMapper;
  private final QualityGateService qualityGateService;
  private final ReportService reportService;

  private final QualityGateStatusCalculator qualityGateStatusCalculator;

  @Autowired
  public OpenApiResultListener(
    ApiTestResultMapper apiTestResultMapper,
    QualityGateService qualityGateService,
    ReportService reportService
  ) {
    this(
      apiTestResultMapper,
      qualityGateService,
      reportService,
      new QualityGateStatusCalculator()
    );
  }

  @VisibleForTesting
  OpenApiResultListener(
    ApiTestResultMapper apiTestResultMapper,
    QualityGateService qualityGateService,
    ReportService reportService,
    QualityGateStatusCalculator qualityGateStatusCalculator
  ) {
    this.apiTestResultMapper = apiTestResultMapper;
    this.qualityGateService = qualityGateService;
    this.reportService = reportService;
    this.qualityGateStatusCalculator = qualityGateStatusCalculator;
  }

  @Transactional
  @KafkaListener(
    groupId = "${" + CONSUMER_GROUP_ID + ":" + DEFAULT_CONSUMER_GROUP_ID + "}",
    topics = { "${" + OPENAPI_CALCULATION_RESPONSE_TOPIC + "}" }
  )
  public void persistOpenApiCoverageResponseIfReportIsPresent(
    @Header(name = RECEIVED_KEY) UUID key,
    @Payload OpenApiCoverageResponseEvent openApiCoverageResponseEvent
  ) {
    reportService
      .findReportByCalculationId(key)
      .map(report ->
        new QualityGateConfigurationParameters(
          report,
          qualityGateService
            .findQualityGateConfigByName(report.getQualityGateConfigName())
            .orElseThrow(() ->
              new IllegalStateException(
                format(
                  "Unreachable state, Quality-Gate configuration '%s' must exist at this point!",
                  report.getQualityGateConfigName()
                )
              )
            )
        )
      )
      .ifPresent(configurationParameters ->
        calculateAndPersistQualityGateReport(
          configurationParameters,
          openApiCoverageResponseEvent.apiInformation(),
          apiTestResultMapper.fromDtos(
            openApiCoverageResponseEvent.openApiCriteria()
          )
        )
      );
  }

  private void calculateAndPersistQualityGateReport(
    QualityGateConfigurationParameters configurationParameters,
    ApiInformation apiInformation,
    Set<ApiTestResult> openApiTestCriteria
  ) {
    try {
      var qualityGateReport = calculateQualityGateReport(
        configurationParameters,
        apiInformation,
        openApiTestCriteria
      );
      reportService.update(qualityGateReport);
    } catch (Exception e) {
      logger.error(
        "Failed to persist quality-gate report: {}",
        configurationParameters,
        e
      );
    }
  }

  private QualityGateReport calculateQualityGateReport(
    QualityGateConfigurationParameters configurationParameters,
    ApiInformation apiInformation,
    Set<ApiTestResult> openApiTestCriteria
  ) {
    var qualityGateReport = configurationParameters.qualityGateReport();

    var calculationResult = new OpenApiReportCalculator(
      qualityGateReport,
      configurationParameters.qualityGateConfig().getOpenApiCriteria(),
      openApiTestCriteria
    ).calculate();

    qualityGateReport = qualityGateReport.withReportStatus(
      calculationResult.status()
    );

    qualityGateReport
      .getApiTests()
      .stream()
      .filter(
        apiTest ->
          apiTest.getServiceName().equals(apiInformation.getServiceName()) &&
          apiTest.getApiName().equals(apiInformation.getApiName()) &&
          (isNull(apiTest.getApiVersion()) ||
            apiTest.getApiVersion().equals(apiInformation.getApiVersion()))
      )
      .findFirst()
      .ifPresent(apiTest ->
        apiTest.getApiTestResults().addAll(calculationResult.apiTestResults())
      );

    return qualityGateStatusCalculator.withUpdatedReportStatus(
      qualityGateReport
    );
  }

  private record QualityGateConfigurationParameters(
    QualityGateReport qualityGateReport,
    QualityGateConfig qualityGateConfig
  ) {}
}

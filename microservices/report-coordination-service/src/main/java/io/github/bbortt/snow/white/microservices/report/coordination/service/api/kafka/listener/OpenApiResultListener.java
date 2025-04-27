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
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.OpenApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.OpenApiReportCalculator;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiResultListener {

  private final OpenApiTestResultMapper openApiTestResultMapper;
  private final QualityGateService qualityGateService;
  private final ReportService reportService;

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
          openApiTestResultMapper.fromDto(
            openApiCoverageResponseEvent.openApiCriteria()
          )
        )
      );
  }

  private void calculateAndPersistQualityGateReport(
    QualityGateConfigurationParameters configurationParameters,
    Set<OpenApiTestResult> openApiTestCriteria
  ) {
    try {
      doCalculateAndPersistQualityGateReport(
        configurationParameters,
        openApiTestCriteria
      );
    } catch (Exception e) {
      logger.error(
        "Failed to persist quality-gate report: {}",
        configurationParameters,
        e
      );
    }
  }

  private void doCalculateAndPersistQualityGateReport(
    QualityGateConfigurationParameters configurationParameters,
    Set<OpenApiTestResult> openApiTestCriteria
  ) {
    var qualityGateReport = configurationParameters.qualityGateReport();

    var calculationResult = new OpenApiReportCalculator(
      qualityGateReport,
      configurationParameters.qualityGateConfig().getOpenapiCriteria(),
      openApiTestCriteria
    ).calculate();

    qualityGateReport = qualityGateReport
      .withOpenApiCoverageStatus(calculationResult.status())
      .withOpenApiTestResults(calculationResult.openApiTestResults());

    reportService.update(qualityGateReport.withUpdatedReportStatus());
  }

  private record QualityGateConfigurationParameters(
    QualityGateReport qualityGateReport,
    QualityGateConfig qualityGateConfig
  ) {}
}

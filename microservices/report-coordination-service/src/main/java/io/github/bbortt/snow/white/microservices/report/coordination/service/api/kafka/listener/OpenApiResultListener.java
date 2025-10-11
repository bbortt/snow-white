/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka.listener;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.CONSUMER_GROUP_ID;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.DEFAULT_CONSUMER_GROUP_ID;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.OPENAPI_CALCULATION_RESPONSE_TOPIC;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.mapper.ApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
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

  private final ApiInformationFilter apiInformationFilter;
  private final ApiTestResultLinker apiTestResultLinker;
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
      new ApiInformationFilter(),
      new ApiTestResultLinker(),
      new QualityGateStatusCalculator()
    );
  }

  @VisibleForTesting
  OpenApiResultListener(
    ApiTestResultMapper apiTestResultMapper,
    QualityGateService qualityGateService,
    ReportService reportService,
    ApiInformationFilter apiInformationFilter,
    ApiTestResultLinker apiTestResultLinker,
    QualityGateStatusCalculator qualityGateStatusCalculator
  ) {
    this.apiTestResultMapper = apiTestResultMapper;
    this.qualityGateService = qualityGateService;
    this.reportService = reportService;
    this.apiInformationFilter = apiInformationFilter;
    this.apiTestResultLinker = apiTestResultLinker;
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
  ) throws QualityGateNotFoundException {
    var reportByCalculationId = reportService.findReportByCalculationId(key);

    if (reportByCalculationId.isEmpty()) {
      logger.warn(
        "Received OpenAPI coverage response for unknown calculation ID: {}",
        key
      );
      return;
    }

    var qualityGateReport = reportByCalculationId.get();

    var apiInformation = openApiCoverageResponseEvent.apiInformation();
    var apiTest =
      apiInformationFilter.findApiTestMatchingApiInformationInQualityGateReport(
        qualityGateReport,
        apiInformation
      );

    var qualityGateConfig = qualityGateService.findQualityGateConfigByName(
      qualityGateReport.getQualityGateConfigName()
    );

    apiTestResultLinker.addResultsToApiTest(
      apiTest,
      qualityGateConfig.getOpenApiCriteria(),
      apiTestResultMapper.fromDtos(
        openApiCoverageResponseEvent.openApiCriteria()
      )
    );

    qualityGateReport = qualityGateStatusCalculator.withUpdatedReportStatus(
      qualityGateReport
    );

    reportService.update(qualityGateReport);
  }
}

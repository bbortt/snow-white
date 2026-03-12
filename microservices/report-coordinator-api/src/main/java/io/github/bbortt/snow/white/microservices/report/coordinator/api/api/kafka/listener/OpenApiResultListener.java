/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static io.github.bbortt.snow.white.commons.kafka.OtelPropagators.KAFKA_HEADERS_GETTER;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.CONSUMER_GROUP_ID;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.DEFAULT_CONSUMER_GROUP_ID;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties.OpenapiCalculationResponse.OPENAPI_CALCULATION_RESPONSE_TOPIC;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class OpenApiResultListener {

  private final OpenTelemetry openTelemetry;

  private final ApiTestResultMapper apiTestResultMapper;
  private final QualityGateService qualityGateService;
  private final ReportService reportService;

  private final ApiInformationFilter apiInformationFilter;
  private final ApiTestResultLinker apiTestResultLinker;
  private final QualityGateStatusCalculator qualityGateStatusCalculator;

  @Autowired
  public OpenApiResultListener(
    OpenTelemetry openTelemetry,
    ApiTestResultMapper apiTestResultMapper,
    QualityGateService qualityGateService,
    ReportService reportService
  ) {
    this(
      openTelemetry,
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
    OpenTelemetry openTelemetry,
    ApiTestResultMapper apiTestResultMapper,
    QualityGateService qualityGateService,
    ReportService reportService,
    ApiInformationFilter apiInformationFilter,
    ApiTestResultLinker apiTestResultLinker,
    QualityGateStatusCalculator qualityGateStatusCalculator
  ) {
    this.openTelemetry = openTelemetry;
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
    ConsumerRecord<
      String,
      OpenApiCoverageResponseEvent
    > openApiCoverageResponseEventConsumerRecord
  ) throws QualityGateNotFoundException {
    var extractedContext = extractTraceContextFromIncomingHeaders(
      openApiCoverageResponseEventConsumerRecord.headers()
    );

    try (var _ = extractedContext.makeCurrent()) {
      updateExistingQualityGateReport(
        UUID.fromString(openApiCoverageResponseEventConsumerRecord.key()),
        openApiCoverageResponseEventConsumerRecord.value()
      );
    }
  }

  private void updateExistingQualityGateReport(
    UUID key,
    OpenApiCoverageResponseEvent openApiCoverageResponseEvent
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

    apiTestResultLinker.addApiTestResultsToApiTest(
      apiTestResultMapper.fromDtos(
        openApiCoverageResponseEvent.openApiCriteria(),
        apiTest
      ),
      apiTest,
      qualityGateConfig.getOpenApiCriteria()
    );

    qualityGateReport = qualityGateStatusCalculator.withUpdatedReportStatus(
      qualityGateReport
    );

    reportService.update(qualityGateReport);
  }

  private @NonNull Context extractTraceContextFromIncomingHeaders(
    Headers nativeHeaders
  ) {
    return openTelemetry
      .getPropagators()
      .getTextMapPropagator()
      .extract(Context.current(), nativeHeaders, KAFKA_HEADERS_GETTER);
  }
}

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
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiResultListener {

  private final OpenTelemetry openTelemetry;
  private final ReportService reportService;

  @Transactional
  @KafkaListener(
    groupId = "${" + CONSUMER_GROUP_ID + ":" + DEFAULT_CONSUMER_GROUP_ID + "}",
    topics = { "${" + OPENAPI_CALCULATION_RESPONSE_TOPIC + "}" }
  )
  public void onOpenApiCoverageResponse(
    ConsumerRecord<
      String,
      OpenApiCoverageResponseEvent
    > openApiCoverageResponseEventConsumerRecord
  ) throws QualityGateNotFoundException {
    var extractedContext = extractTraceContextFromIncomingHeaders(
      openApiCoverageResponseEventConsumerRecord.headers()
    );

    try (var _ = extractedContext.makeCurrent()) {
      reportService.updateReportWithOpenApiCoverageResults(
        UUID.fromString(openApiCoverageResponseEventConsumerRecord.key()),
        openApiCoverageResponseEventConsumerRecord.value()
      );
    }
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

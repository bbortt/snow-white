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
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.TestResultForUnknownApiException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiResultListener {

  private final OpenTelemetry openTelemetry;
  private final ReportService reportService;
  private final ReportCoordinationServiceProperties properties;

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
  ) {
    var extractedContext = extractTraceContextFromIncomingHeaders(
      openApiCoverageResponseEventConsumerRecord.headers()
    );

    var calculationId = UUID.fromString(
      openApiCoverageResponseEventConsumerRecord.key()
    );

    var openApiCoverageResponseEvent =
      openApiCoverageResponseEventConsumerRecord.value();
    try (var _ = extractedContext.makeCurrent()) {
      reportService.updateReportWithOpenApiCoverageResults(
        calculationId,
        openApiCoverageResponseEvent
      );
    } catch (TestResultForUnknownApiException e) {
      logger.error("Failed to update OpenAPI result '{}'!", calculationId, e);
    } catch (RuntimeException runtimeException) {
      logger.error(
        "Failed to persist OpenAPI result '{}'! Rejecting Kafka message for retry.",
        calculationId,
        runtimeException
      );

      if (
        isLastDeliveryAttempt(
          openApiCoverageResponseEventConsumerRecord.headers()
        )
      ) {
        reportService
          .findReportByCalculationId(calculationId)
          .ifPresent(report ->
            reportService.handleExceptionalResponse(
              report,
              openApiCoverageResponseEvent.withErrorMessage(
                runtimeException.getMessage()
              )
            )
          );
      }

      throw runtimeException;
    }
  }

  private boolean isLastDeliveryAttempt(Headers headers) {
    var header = headers.lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
    int attempt = header != null ? ByteBuffer.wrap(header.value()).getInt() : 1;
    int maxAttempts =
      Math.max(
        Optional.ofNullable(
          properties.getOpenapiCalculationResponse().getMaxRetries()
        ).orElse(2),
        0
      ) + 1;
    return attempt >= maxAttempts;
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

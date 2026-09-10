/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
class QualityGateCalculationRequestDispatcher {

  private final KafkaTemplate<
    @NonNull String,
    @NonNull QualityGateCalculationRequestEvent
  > kafkaTemplate;

  private final OpenTelemetry openTelemetry;

  private final String calculationRequestTopic;

  public QualityGateCalculationRequestDispatcher(
    KafkaTemplate<
      @NonNull String,
      @NonNull QualityGateCalculationRequestEvent
    > kafkaTemplate,
    OpenTelemetry openTelemetry,
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    this.kafkaTemplate = kafkaTemplate;
    this.openTelemetry = openTelemetry;

    calculationRequestTopic =
      reportCoordinationServiceProperties.getCalculationRequestTopic();
  }

  public void dispatch(
    UUID calculationId,
    ReportParameter reportParameter,
    Set<ApiTest> apiTests
  ) {
    apiTests.forEach(apiTest ->
      dispatchSingle(calculationId, reportParameter, apiTest)
    );
  }

  private void dispatchSingle(
    UUID calculationId,
    ReportParameter reportParameter,
    ApiTest apiTest
  ) {
    var qualityGateCalculationRequestEventProducerRecord = new ProducerRecord<>(
      calculationRequestTopic,
      calculationId.toString(),
      QualityGateCalculationRequestEvent.builder()
        .apiInformation(
          ApiInformation.builder()
            .serviceName(apiTest.getServiceName())
            .apiName(apiTest.getApiName())
            .apiVersion(apiTest.getApiVersion())
            .apiType(apiTest.getApiType())
            .build()
        )
        .lookbackWindow(reportParameter.getLookbackWindow())
        .attributeFilters(toAttributeFilters(reportParameter))
        .build()
    );

    openTelemetry
      .getPropagators()
      .getTextMapPropagator()
      .inject(
        Context.current(),
        qualityGateCalculationRequestEventProducerRecord.headers(),
        (headers, key, value) -> headers.add(key, value.getBytes(UTF_8))
      );

    kafkaTemplate.send(qualityGateCalculationRequestEventProducerRecord);
  }

  private static Set<AttributeFilter> toAttributeFilters(
    ReportParameter reportParameter
  ) {
    return reportParameter
      .getAttributeFilters()
      .entrySet()
      .stream()
      .map(entry ->
        new AttributeFilter(entry.getKey(), STRING_EQUALS, entry.getValue())
      )
      .collect(toSet());
  }
}

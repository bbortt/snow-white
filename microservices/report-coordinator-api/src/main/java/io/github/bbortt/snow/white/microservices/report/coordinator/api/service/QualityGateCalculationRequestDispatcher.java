/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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

  @RealizesArch(
    ArchTraceables.ARCH_004_PER_API_TEST_FAN_OUT_KEYED_BY_CALCULATION_ID
  )
  public void dispatch(
    UUID calculationId,
    Instant initiatedAt,
    ReportParameter reportParameter,
    Set<ApiTest> apiTests
  ) {
    apiTests.forEach(apiTest ->
      dispatchSingle(calculationId, initiatedAt, reportParameter, apiTest)
    );
  }

  /**
   * One record per API test, all keyed by the calculation id so the whole fan-out shares a
   * partition, with the active trace context injected into the record headers.
   * <p>
   * Each record carries the waivers of this calculation that govern its own API and no others, so a
   * consumer never has to decide whether an entry was meant for it. The instant the calculation was
   * triggered travels on every one of them: it is the value a waiver's expiry is judged against, and
   * reading it off the record timestamp instead would make that judgement a function of a broker
   * setting.
   */
  @RealizesArch(
    ArchTraceables.ARCH_004_PER_API_TEST_FAN_OUT_KEYED_BY_CALCULATION_ID
  )
  private void dispatchSingle(
    UUID calculationId,
    Instant initiatedAt,
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
        .waivers(toWaivers(reportParameter, apiTest))
        .initiatedAt(initiatedAt)
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

  /**
   * The calculation's waivers narrowed to the one API this record travels for, in document order. A
   * wildcarded version matches every version of its service and API, which is the whole of what the
   * wildcard does - it is resolved here, once, rather than shipped downstream to be interpreted.
   */
  private static @Nullable List<Waiver> toWaivers(
    ReportParameter reportParameter,
    ApiTest apiTest
  ) {
    var waivers = reportParameter
      .getWaivers()
      .stream()
      .filter(waiver ->
        waiver.appliesTo(
          apiTest.getServiceName(),
          apiTest.getApiName(),
          apiTest.getApiVersion()
        )
      )
      .map(waiver ->
        new Waiver(
          waiver.getPath(),
          waiver.getMethod(),
          waiver.getResponseCode(),
          waiver.getParameterName(),
          waiver.getContentType(),
          waiver.getCriterion(),
          waiver.getReason(),
          waiver.getExpires()
        )
      )
      .toList();

    return waivers.isEmpty() ? null : waivers;
  }
}

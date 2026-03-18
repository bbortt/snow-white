/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiResultListenerTest {

  @Mock
  private OpenTelemetry openTelemetryMock;

  @Mock
  private ContextPropagators contextPropagatorsMock;

  @Mock
  private TextMapPropagator textMapPropagatorMock;

  @Mock
  private ReportService reportServiceMock;

  private OpenApiResultListener fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiResultListener(openTelemetryMock, reportServiceMock);
  }

  @Nested
  class OnOpenApiCoverageResponseTest {

    private static final UUID CALCULATION_ID = UUID.fromString(
      "9f679723-a328-47c6-b24e-e16894c675f1"
    );

    private static Headers emptyHeaders() {
      return new RecordHeaders();
    }

    private @NonNull ConsumerRecord<
      String,
      OpenApiCoverageResponseEvent
    > getOpenApiCoverageResponseEventConsumerRecord(
      OpenApiCoverageResponseEvent event,
      Headers headers
    ) {
      var openApiCoverageResponseEventConsumerRecord = new ConsumerRecord<>(
        "snow-white-openapi-calculation-response", // topic
        0, // partition
        0L, // offset
        CALCULATION_ID.toString(), // key
        event // value
      );
      headers.forEach(
        openApiCoverageResponseEventConsumerRecord.headers()::add
      );
      return openApiCoverageResponseEventConsumerRecord;
    }

    @BeforeEach
    void beforeEachSetup() {
      doReturn(contextPropagatorsMock).when(openTelemetryMock).getPropagators();
      doReturn(textMapPropagatorMock)
        .when(contextPropagatorsMock)
        .getTextMapPropagator();
    }

    @Test
    void shouldExtractTraceContextFromIncomingHeaders()
      throws QualityGateNotFoundException {
      var headers = new RecordHeaders();
      headers.add(
        "traceparent",
        "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01".getBytes(
          UTF_8
        )
      );

      var extractedContext = Context.root();
      doReturn(extractedContext)
        .when(textMapPropagatorMock)
        .extract(any(), eq(headers), any());

      var event = new OpenApiCoverageResponseEvent(
        mock(ApiInformation.class),
        emptySet()
      );

      fixture.onOpenApiCoverageResponse(
        getOpenApiCoverageResponseEventConsumerRecord(event, headers)
      );

      ArgumentCaptor<Headers> headersCaptor = captor();
      verify(textMapPropagatorMock).extract(
        any(Context.class),
        headersCaptor.capture(),
        any()
      );
      assertThat(headersCaptor.getValue()).isEqualTo(headers);
    }

    @Test
    void shouldExtractTraceContext_usingHeadersGetter()
      throws QualityGateNotFoundException {
      var traceparentValue = "00-abc123def456abc1-abc1def2abc3def4-01";
      var headers = new RecordHeaders();
      headers.add("traceparent", traceparentValue.getBytes(UTF_8));

      var resolvedTraceparent = new AtomicReference<String>();
      doAnswer(invocation -> {
        TextMapGetter<Headers> getter = invocation.getArgument(2);
        resolvedTraceparent.set(getter.get(headers, "traceparent"));
        return Context.root();
      })
        .when(textMapPropagatorMock)
        .extract(any(), any(Headers.class), any());

      var event = new OpenApiCoverageResponseEvent(
        mock(ApiInformation.class),
        emptySet()
      );

      fixture.onOpenApiCoverageResponse(
        getOpenApiCoverageResponseEventConsumerRecord(event, headers)
      );

      assertThat(resolvedTraceparent.get()).isEqualTo(traceparentValue);
    }

    @Test
    void shouldDelegateToReportService() throws QualityGateNotFoundException {
      doReturn(Context.root())
        .when(textMapPropagatorMock)
        .extract(any(), any(Headers.class), any());

      var event = new OpenApiCoverageResponseEvent(
        mock(ApiInformation.class),
        emptySet()
      );

      fixture.onOpenApiCoverageResponse(
        getOpenApiCoverageResponseEventConsumerRecord(event, emptyHeaders())
      );

      verify(reportServiceMock).updateReportWithOpenApiCoverageResults(
        CALCULATION_ID,
        event
      );
    }
  }
}

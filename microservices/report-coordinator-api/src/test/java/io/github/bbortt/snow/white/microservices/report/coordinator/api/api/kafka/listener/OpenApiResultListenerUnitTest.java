/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.TestData.minimalQualityGateReport;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.TestResultForUnknownApiException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.nio.ByteBuffer;
import java.util.Optional;
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
import org.springframework.kafka.support.KafkaHeaders;

@ExtendWith({ MockitoExtension.class })
class OpenApiResultListenerUnitTest {

  @Mock
  private OpenTelemetry openTelemetryMock;

  @Mock
  private ContextPropagators contextPropagatorsMock;

  @Mock
  private TextMapPropagator textMapPropagatorMock;

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private ReportCoordinationServiceProperties propertiesMock;

  private OpenApiResultListener fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiResultListener(
      openTelemetryMock,
      reportServiceMock,
      propertiesMock
    );
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
    void shouldExtractTraceContextFromIncomingHeaders() {
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

      var event = getEmptyOpenApiCoverageResponseEvent();

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
    void shouldExtractTraceContext_usingHeadersGetter() {
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

      var event = getEmptyOpenApiCoverageResponseEvent();

      fixture.onOpenApiCoverageResponse(
        getOpenApiCoverageResponseEventConsumerRecord(event, headers)
      );

      assertThat(resolvedTraceparent.get()).isEqualTo(traceparentValue);
    }

    @Test
    void shouldDelegateToReportService() {
      withValidOpenTelemetryContext();

      var event = getEmptyOpenApiCoverageResponseEvent();

      fixture.onOpenApiCoverageResponse(
        getOpenApiCoverageResponseEventConsumerRecord(event, emptyHeaders())
      );

      verify(reportServiceMock).updateReportWithOpenApiCoverageResults(
        CALCULATION_ID,
        event
      );
    }

    @Test
    void shouldCatchButNotRetryTestResultForUnknownApiException() {
      withValidOpenTelemetryContext();

      var event = getEmptyOpenApiCoverageResponseEvent();

      doThrow(
        new TestResultForUnknownApiException(
          mock(QualityGateReport.class),
          mock(ApiInformation.class)
        )
      )
        .when(reportServiceMock)
        .updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      fixture.onOpenApiCoverageResponse(
        getOpenApiCoverageResponseEventConsumerRecord(event, emptyHeaders())
      );

      verify(reportServiceMock).updateReportWithOpenApiCoverageResults(
        CALCULATION_ID,
        event
      );
      verifyNoMoreInteractions(reportServiceMock);
    }

    @Test
    void shouldCatchAndThrowAnyOtherExceptionForRetrying() {
      withValidOpenTelemetryContext();
      withMaxRetries(2);

      var event = getEmptyOpenApiCoverageResponseEvent();

      var exception = new IllegalArgumentException("thrown on purpose");
      doThrow(exception)
        .when(reportServiceMock)
        .updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      assertThatThrownBy(() ->
        fixture.onOpenApiCoverageResponse(
          getOpenApiCoverageResponseEventConsumerRecord(event, emptyHeaders())
        )
      ).isEqualTo(exception);

      verify(reportServiceMock, never()).handleExceptionalResponse(
        any(),
        any(OpenApiCoverageResponseEvent.class)
      );
    }

    @Test
    void shouldCatchAndPersistExhaustedRetryException() {
      withValidOpenTelemetryContext();
      withMaxRetries(2);

      var event = getEmptyOpenApiCoverageResponseEvent();

      var rootCause = "thrown on purpose";
      var exception = new IllegalArgumentException(rootCause);
      doThrow(exception)
        .when(reportServiceMock)
        .updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      var qualityGateReport = minimalQualityGateReport(CALCULATION_ID);
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(CALCULATION_ID);

      assertThatThrownBy(() ->
        fixture.onOpenApiCoverageResponse(
          getOpenApiCoverageResponseEventConsumerRecord(
            event,
            headersWithDeliveryAttempt(3)
          )
        )
      ).isEqualTo(exception);

      ArgumentCaptor<
        OpenApiCoverageResponseEvent
      > openApiCoverageResponseEventArgumentCaptor = captor();
      verify(reportServiceMock).handleExceptionalResponse(
        eq(qualityGateReport),
        openApiCoverageResponseEventArgumentCaptor.capture()
      );

      assertThat(openApiCoverageResponseEventArgumentCaptor.getValue())
        .isNotNull()
        .extracting(OpenApiCoverageResponseEvent::errorMessage)
        .isEqualTo(rootCause);
    }

    private void withValidOpenTelemetryContext() {
      doReturn(Context.root())
        .when(textMapPropagatorMock)
        .extract(any(), any(Headers.class), any());
    }

    private void withMaxRetries(int maxRetries) {
      var openapiProps =
        new ReportCoordinationServiceProperties.OpenapiCalculationResponse();
      openapiProps.setMaxRetries(maxRetries);
      doReturn(openapiProps)
        .when(propertiesMock)
        .getOpenapiCalculationResponse();
    }

    private static Headers headersWithDeliveryAttempt(int attempt) {
      var headers = new RecordHeaders();
      headers.add(
        KafkaHeaders.DELIVERY_ATTEMPT,
        ByteBuffer.allocate(4).putInt(attempt).array()
      );
      return headers;
    }

    private static @NonNull OpenApiCoverageResponseEvent getEmptyOpenApiCoverageResponseEvent() {
      return new OpenApiCoverageResponseEvent(
        mock(ApiInformation.class),
        emptySet()
      );
    }
  }
}

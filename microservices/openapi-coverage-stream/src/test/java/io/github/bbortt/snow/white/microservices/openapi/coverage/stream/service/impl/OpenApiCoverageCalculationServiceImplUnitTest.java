/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.RequiredAttributeKeyService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.TelemetryBackendUnavailableException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageCalculationServiceImplUnitTest {

  @Mock
  private OpenApiService openApiServiceMock;

  @Mock
  private OpenTelemetryService openTelemetryServiceMock;

  @Mock
  private OpenApiCoverageService openApiCoverageServiceMock;

  @Mock
  private RequiredAttributeKeyService requiredAttributeKeyServiceMock;

  @InjectMocks
  private OpenApiCoverageCalculationServiceImpl fixture;

  private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 15);
  private static final Instant TRIGGERED_AT = EXPIRES.minusDays(7)
    .atStartOfDay(UTC)
    .toInstant();

  private static Waiver waiver() {
    return new Waiver(
      "/pets/{petId}",
      "GET",
      "500",
      null,
      null,
      null,
      "mocked downstream cannot be made to fail",
      EXPIRES
    );
  }

  @Nested
  class FetchOpenApiSpecificationTest {

    @Test
    void shouldFetchSpecification()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var apiInformation = defaultApiInformation();
      var requestEvent = QualityGateCalculationRequestEvent.builder()
        .apiInformation(apiInformation)
        .lookbackWindow("1h")
        .attributeFilters(
          Set.of(
            new AttributeFilter(
              "key",
              AttributeFilterOperator.STRING_EQUALS,
              "value"
            )
          )
        )
        .build();
      var openAPI = mock(OpenAPI.class);

      doReturn(openAPI)
        .when(openApiServiceMock)
        .findAndParseOpenApi(apiInformation);

      var result = fixture.fetchOpenApiSpecification("key", requestEvent);

      assertThat(result).isNotNull();
      assertThat(result.apiInformation()).isEqualTo(apiInformation);
      assertThat(result.openAPI()).isEqualTo(openAPI);
      assertThat(result.lookbackWindow()).isEqualTo("1h");
      assertThat(result.attributeFilters()).hasSize(1);
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldTakeBothHalvesOfTheWaiverContextOffTheEvent()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var apiInformation = defaultApiInformation();
      var requestEvent = QualityGateCalculationRequestEvent.builder()
        .apiInformation(apiInformation)
        .lookbackWindow("1h")
        .waivers(List.of(waiver()))
        .initiatedAt(TRIGGERED_AT)
        .build();

      doReturn(mock(OpenAPI.class))
        .when(openApiServiceMock)
        .findAndParseOpenApi(apiInformation);

      var result = fixture.fetchOpenApiSpecification("key", requestEvent);

      assertThat(result.waiverContext()).isEqualTo(
        new WaiverContext(List.of(waiver()), TRIGGERED_AT)
      );
    }

    @Test
    void shouldCarryAnEmptyWaiverContext_whenTheEventNamedNoWaiver()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var apiInformation = defaultApiInformation();
      var requestEvent = QualityGateCalculationRequestEvent.builder()
        .apiInformation(apiInformation)
        .lookbackWindow("1h")
        .build();

      doReturn(mock(OpenAPI.class))
        .when(openApiServiceMock)
        .findAndParseOpenApi(apiInformation);

      var result = fixture.fetchOpenApiSpecification("key", requestEvent);

      assertThat(result.waiverContext()).isEqualTo(WaiverContext.none());
    }

    public static Stream<Exception> shouldPropagateExceptions() {
      return Stream.of(
        mock(OpenApiNotIndexedException.class),
        mock(UnparseableOpenApiException.class)
      );
    }

    @MethodSource
    @ParameterizedTest
    void shouldPropagateExceptions(Exception cause)
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      doThrow(cause).when(openApiServiceMock).findAndParseOpenApi(any());

      var qualityGateCalculationRequestEvent =
        QualityGateCalculationRequestEvent.builder()
          .apiInformation(mock(ApiInformation.class))
          .lookbackWindow("1h")
          .build();

      assertThatThrownBy(() ->
        fixture.fetchOpenApiSpecification(
          "key",
          qualityGateCalculationRequestEvent
        )
      ).isEqualTo(cause);
    }
  }

  @Nested
  class EnrichWithOpenTelemetryDataTest {

    @Test
    void shouldEnrichWithTelemetryData()
      throws TelemetryBackendUnavailableException {
      var apiInformation = defaultApiInformation();
      var openAPI = mock(OpenAPI.class);
      var context = new OpenApiTestContext(
        apiInformation,
        openAPI,
        "1h",
        Set.of()
      );
      var telemetryData = Set.of(mock(OpenTelemetryData.class));

      var requiredAttributeKeys = Set.of("http.request.method", "url.path");
      doReturn(requiredAttributeKeys)
        .when(requiredAttributeKeyServiceMock)
        .requiredAttributeKeys(openAPI);

      doReturn(telemetryData)
        .when(openTelemetryServiceMock)
        .findOpenTelemetryTracingData(
          eq(apiInformation),
          anyLong(),
          eq("1h"),
          any(),
          eq(requiredAttributeKeys)
        );

      var result = fixture.enrichWithOpenTelemetryData(context, 12345L);

      assertThat(result.openTelemetryData()).isEqualTo(telemetryData);
    }

    /**
     * The record timestamp and the triggering instant are different clocks with different meanings:
     * the first is when the broker accepted the message, which is a retention and a producer
     * setting, and the second is when somebody asked for this report. Only the second can decide a
     * waiver's expiry - a replay off the same topic a month later arrives with a new record
     * timestamp and must still reach the verdict the original run reached.
     * <p>
     * The timestamp here sits a month past the waiver's expiry, so reading expiry off it would turn
     * a waiver in force into an expired one.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldJudgeTheWaiverAgainstTheTriggeringInstantRatherThanTheRecordTimestamp()
      throws OpenApiNotIndexedException, UnparseableOpenApiException, TelemetryBackendUnavailableException {
      var apiInformation = defaultApiInformation();
      var openAPI = mock(OpenAPI.class);
      var requestEvent = QualityGateCalculationRequestEvent.builder()
        .apiInformation(apiInformation)
        .lookbackWindow("1h")
        .waivers(List.of(waiver()))
        .initiatedAt(TRIGGERED_AT)
        .build();
      var recordTimestamp = EXPIRES.plusMonths(1)
        .atStartOfDay(UTC)
        .toInstant()
        .toEpochMilli();

      doReturn(openAPI)
        .when(openApiServiceMock)
        .findAndParseOpenApi(apiInformation);
      doReturn(Set.of(mock(OpenTelemetryData.class)))
        .when(openTelemetryServiceMock)
        .findOpenTelemetryTracingData(
          eq(apiInformation),
          anyLong(),
          eq("1h"),
          any(),
          any()
        );

      var result = fixture.enrichWithOpenTelemetryData(
        fixture.fetchOpenApiSpecification("key", requestEvent),
        recordTimestamp
      );

      assertThat(result.waiverContext().initiatedAt()).isEqualTo(TRIGGERED_AT);
      assertThat(result.waiverContext().inForce()).containsExactly(waiver());
      assertThat(result.waiverContext().expired()).isEmpty();

      verify(openTelemetryServiceMock).findOpenTelemetryTracingData(
        eq(apiInformation),
        eq(recordTimestamp),
        eq("1h"),
        any(),
        any()
      );
    }

    @Test
    void shouldPropagateTelemetryBackendUnavailableException()
      throws TelemetryBackendUnavailableException {
      var apiInformation = defaultApiInformation();
      var context = new OpenApiTestContext(
        apiInformation,
        mock(OpenAPI.class),
        "1h",
        Set.of()
      );

      var cause = new TelemetryBackendUnavailableException(
        "Grafana Tempo",
        new IllegalStateException("502 Bad Gateway")
      );
      doThrow(cause)
        .when(openTelemetryServiceMock)
        .findOpenTelemetryTracingData(
          eq(apiInformation),
          anyLong(),
          eq("1h"),
          any(),
          any()
        );

      assertThatThrownBy(() ->
        fixture.enrichWithOpenTelemetryData(context, 12345L)
      ).isSameAs(cause);
    }
  }

  @Nested
  class CalculateCoverageTest {

    @Test
    void shouldCalculateCoverage() {
      var context = new OpenApiTestContext(
        mock(ApiInformation.class),
        mock(OpenAPI.class),
        "1h",
        Set.of()
      );
      var testResults = Set.of(mock(OpenApiTestResult.class));

      doReturn(testResults)
        .when(openApiCoverageServiceMock)
        .calculateCoverage(context);

      var result = fixture.calculateCoverage(context);

      assertThat(result.openApiTestResults()).isEqualTo(testResults);
    }
  }

  @Nested
  class BuildResponseEventTest {

    @Test
    void shouldBuildResponseEvent() {
      var apiInformation = defaultApiInformation();
      var testResults = Set.of(mock(OpenApiTestResult.class));
      var context = new OpenApiTestContext(
        apiInformation,
        mock(OpenAPI.class),
        "1h",
        Set.of(),
        WaiverContext.none(),
        null,
        testResults
      );

      var result = fixture.buildResponseEvent(context);

      assertThat(result.getApiType()).isEqualTo(OPENAPI);
      assertThat(result.apiInformation()).isEqualTo(apiInformation);
      assertThat(result.openApiTestResults()).isEqualTo(testResults);
    }
  }
}

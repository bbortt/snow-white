/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageCalculationServiceImplTest {

  @Mock
  private OpenApiService openApiServiceMock;

  @Mock
  private OpenTelemetryService openTelemetryServiceMock;

  @Mock
  private OpenApiCoverageService openApiCoverageServiceMock;

  private OpenApiCoverageCalculationServiceImpl fixture;

  @BeforeEach
  void setUp() {
    fixture = new OpenApiCoverageCalculationServiceImpl(
      openApiServiceMock,
      openTelemetryServiceMock,
      openApiCoverageServiceMock
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
      assertThat(result.fluxAttributeFilters()).hasSize(1);
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
    void shouldEnrichWithTelemetryData() {
      var apiInformation = defaultApiInformation();
      var context = new OpenApiTestContext(
        apiInformation,
        mock(OpenAPI.class),
        "1h",
        Set.of()
      );
      var telemetryData = Set.of(mock(OpenTelemetryData.class));

      doReturn(telemetryData)
        .when(openTelemetryServiceMock)
        .findOpenTelemetryTracingData(
          eq(apiInformation),
          anyLong(),
          eq("1h"),
          any()
        );

      var result = fixture.enrichWithOpenTelemetryData(context, 12345L);

      assertThat(result.openTelemetryData()).isEqualTo(telemetryData);
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

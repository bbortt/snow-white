/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageServiceTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private OpenApiCoverageCalculationCoordinator openApiCoverageCalculationCoordinatorMock;

  @Mock
  private OpenTelemetryService openTelemetryServiceMock;

  private OpenApiCoverageService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCoverageService(
      openApiCoverageCalculationCoordinatorMock,
      openTelemetryServiceMock
    );
  }

  @Nested
  class GatherDataAndCalculateCoverage {

    private static final String OTEL_SERVICE_NAME = "otelServiceName";
    private static final String LOOKBACK_WINDOW = "lookbackWindow";

    @Mock
    private OpenAPI openAPIMock;

    private OpenApiService.OpenApiCoverageRequest openedApiCoverageRequest;

    @BeforeEach
    void beforeEachSetup() {
      openedApiCoverageRequest = new OpenApiService.OpenApiCoverageRequest(
        new OpenApiService.OpenApiIdentifier(OTEL_SERVICE_NAME, null, null),
        openAPIMock,
        LOOKBACK_WINDOW
      );
    }

    @Test
    void shouldReturnCalculatedCoverage() throws JsonProcessingException {
      var spanId = "5f2d3c8b9e1a0f4d";
      var traceId = "6e0c63257de34c92b8b045a7c3c2e3fc";
      var attributes = objectMapper.readTree(
        // language=json
        """
        {"http.request.method": "GET","url.path":"/api/rest/v1/foo" }
        """
      );

      doReturn(
        singletonList(new OpenTelemetryData(spanId, traceId, attributes))
      )
        .when(openTelemetryServiceMock)
        .findTracingData(eq(OTEL_SERVICE_NAME), eq(LOOKBACK_WINDOW), anyList());

      var paths = new Paths();
      paths.addPathItem(
        "/api/rest/v1/foo",
        new PathItem().get(mock(Operation.class))
      );

      doReturn(paths).when(openAPIMock).getPaths();

      Set<OpenApiTestResult> openApiTestResults = emptySet();
      ArgumentCaptor<Map<String, Operation>> pathToOpenAPIOperationMapCaptor =
        captor();
      ArgumentCaptor<
        Map<String, List<OpenTelemetryData>>
      > pathToTelemetryMapCaptor = captor();
      doReturn(openApiTestResults)
        .when(openApiCoverageCalculationCoordinatorMock)
        .calculate(
          pathToOpenAPIOperationMapCaptor.capture(),
          pathToTelemetryMapCaptor.capture()
        );

      Set<OpenApiTestResult> result = fixture.gatherDataAndCalculateCoverage(
        openedApiCoverageRequest
      );

      assertThat(result).isEqualTo(openApiTestResults);

      assertThat(pathToOpenAPIOperationMapCaptor.getValue())
        .isNotNull()
        .hasEntrySatisfying("GET_/api/rest/v1/foo", v ->
          assertThat(v).isInstanceOf(Operation.class)
        );

      assertThat(pathToTelemetryMapCaptor.getValue())
        .isNotNull()
        .hasEntrySatisfying("GET_/api/rest/v1/foo", v ->
          assertThat(v)
            .hasSize(1)
            .first()
            .satisfies(
              d -> assertThat(d.spanId()).isEqualTo(spanId),
              d -> assertThat(d.traceId()).isEqualTo(traceId),
              d -> assertThat(d.attributes()).isEqualTo(attributes)
            )
        );
    }

    @Test
    void shouldReturnEmptySet_whenNoTelemetryDataGathered() {
      doReturn(emptyList())
        .when(openTelemetryServiceMock)
        .findTracingData(eq(OTEL_SERVICE_NAME), eq(LOOKBACK_WINDOW), anyList());

      Set<OpenApiTestResult> result = fixture.gatherDataAndCalculateCoverage(
        openedApiCoverageRequest
      );

      assertThat(result).isEmpty();

      verifyNoInteractions(openAPIMock);
      verifyNoInteractions(openApiCoverageCalculationCoordinatorMock);
    }
  }
}

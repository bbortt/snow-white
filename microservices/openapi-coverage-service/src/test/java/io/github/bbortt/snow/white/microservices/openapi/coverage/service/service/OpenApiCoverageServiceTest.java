/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.HEAD;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.OPTIONS;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PATCH;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageServiceTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private OpenApiCoverageCalculationCoordinator openApiCoverageCalculationCoordinatorMock;

  private OpenApiCoverageService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCoverageService(
      openApiCoverageCalculationCoordinatorMock
    );
  }

  @Nested
  class TestOpenApi {

    private static final String LOOKBACK_WINDOW = "lookbackWindow";

    @Mock
    private OpenAPI openAPIMock;

    private OpenApiTestContext openApiTestContext;

    @BeforeEach
    void beforeEachSetup() {
      openApiTestContext = new OpenApiTestContext(
        ApiInformation.builder()
          .serviceName("serviceName")
          .apiName("apiName")
          .build(),
        openAPIMock,
        LOOKBACK_WINDOW,
        null
      );
    }

    public static Stream<Arguments> shouldReturnCalculatedCoverage() {
      return Stream.of(
        arguments(GET, new PathItem().get(mock(Operation.class))),
        arguments(POST, new PathItem().post(mock(Operation.class))),
        arguments(PUT, new PathItem().put(mock(Operation.class))),
        arguments(DELETE, new PathItem().delete(mock(Operation.class))),
        arguments(PATCH, new PathItem().patch(mock(Operation.class))),
        arguments(HEAD, new PathItem().head(mock(Operation.class))),
        arguments(OPTIONS, new PathItem().options(mock(Operation.class)))
      );
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnCalculatedCoverage(
      PathItem.HttpMethod httpMethod,
      PathItem pathItem
    ) throws JsonProcessingException {
      var spanId = "5f2d3c8b9e1a0f4d";
      var traceId = "6e0c63257de34c92b8b045a7c3c2e3fc";
      var attributes = objectMapper.readTree(
        // language=json
        """
        {"http.request.method": "%s","url.path":"/api/rest/v1/foo" }
        """.formatted(httpMethod.name())
      );

      openApiTestContext = openApiTestContext.withOpenTelemetryData(
        Set.of(new OpenTelemetryData(spanId, traceId, attributes))
      );

      var paths = new Paths();
      paths.addPathItem("/api/rest/v1/foo", pathItem);

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

      Set<OpenApiTestResult> result = fixture.testOpenApi(openApiTestContext);

      assertThat(result).isEqualTo(openApiTestResults);

      assertThat(pathToOpenAPIOperationMapCaptor.getValue())
        .isNotNull()
        .hasEntrySatisfying(httpMethod.name() + "_/api/rest/v1/foo", v ->
          assertThat(v).isInstanceOf(Operation.class)
        );

      assertThat(pathToTelemetryMapCaptor.getValue())
        .isNotNull()
        .hasEntrySatisfying(httpMethod.name() + "_/api/rest/v1/foo", v ->
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

    public static Stream<
      Set<OpenTelemetryData>
    > shouldReturnEmptySet_whenNoTelemetryDataGathered() {
      return Stream.of(null, emptySet());
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnEmptySet_whenNoTelemetryDataGathered(
      @Nullable Set<OpenTelemetryData> openTelemetryData
    ) {
      openApiTestContext = openApiTestContext.withOpenTelemetryData(
        openTelemetryData
      );

      Set<OpenApiTestResult> result = fixture.testOpenApi(openApiTestContext);

      assertThat(result).isEmpty();

      verifyNoInteractions(openAPIMock);
      verifyNoInteractions(openApiCoverageCalculationCoordinatorMock);
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class PathCoverageCalculatorTest {

  @Mock
  private Operation operationMock;

  private PathCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new PathCoverageCalculator();
  }

  @Nested
  class Accepts {

    @Test
    void shouldReturnTrue_whenPathCoverage() {
      boolean result = fixture.accepts(PATH_COVERAGE);

      assertThat(result).isTrue();
    }

    @ParameterizedTest
    @EnumSource(OpenApiCriteria.class)
    void shouldReturnFalse_whenNotPathCoverage(
      OpenApiCriteria openApiCriteria
    ) {
      if (PATH_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class Calculates {

    @Test
    void shouldReturn100Percent_whenPathIngoringMethodsCovered() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(mock(OpenTelemetryData.class))
      );
      pathToTelemetryMap.put(
        "POST_/api/v1/users",
        singletonList(mock(OpenTelemetryData.class))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn100Percent_whenHalfMethodsCovered() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("POST_/api/v1/users", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(mock(OpenTelemetryData.class))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn0Percent_whenNoPathsCovered() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("POST_/api/v1/users", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following resources (ignoring request methods) are uncovered: `/api/v1/users`"
          )
      );
    }

    @Test
    void shouldHandleEmptyOperationsMap() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(mock(OpenTelemetryData.class))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PATH_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageCalculationCoordinatorTest {

  @Mock
  private OpenApiCoverageCalculator openApiCoverageCalculatorMock;

  @Nested
  class Calculate {

    private final Map<String, Operation> pathToOpenAPIOperationMap =
      new HashMap<>();
    private final Map<String, List<OpenTelemetryData>> pathToTelemetryMap =
      new HashMap<>();

    @Test
    void shouldInvokeEachOpenApiCoverageCalculator() {
      pathToOpenAPIOperationMap.put("GET_/foo", mock(Operation.class));
      pathToTelemetryMap.put("GET_/foo", emptyList());

      var coverage = BigDecimal.ONE;
      var openApiCriteriaResult = new OpenApiTestResult(
        PATH_COVERAGE,
        coverage,
        Duration.ofSeconds(1),
        "additionalInformation"
      );

      doReturn(openApiCriteriaResult)
        .when(openApiCoverageCalculatorMock)
        .calculate(pathToOpenAPIOperationMap, pathToTelemetryMap);

      var fixture = new OpenApiCoverageCalculationCoordinator(
        singletonList(openApiCoverageCalculatorMock)
      );
      Set<OpenApiTestResult> result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).containsExactly(openApiCriteriaResult);
    }

    @Test
    void shouldReturnEmptySet_withoutAnyOpenApiOperation() {
      var fixture = new OpenApiCoverageCalculationCoordinator(
        singletonList(openApiCoverageCalculatorMock)
      );

      Set<OpenApiTestResult> result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).isEmpty();

      verifyNoInteractions(openApiCoverageCalculatorMock);
    }

    @Test
    void shouldReturnEmptySet_withoutAnyTelemetryData() {
      var fixture = new OpenApiCoverageCalculationCoordinator(
        singletonList(openApiCoverageCalculatorMock)
      );

      Set<OpenApiTestResult> result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).isEmpty();

      verifyNoInteractions(openApiCoverageCalculatorMock);
    }

    @Test
    void shouldReturnEmptySet_withoutAnyOpenApiCoverageCalculator() {
      var fixture = new OpenApiCoverageCalculationCoordinator(emptyList());

      Set<OpenApiTestResult> result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).isEmpty();
    }
  }
}

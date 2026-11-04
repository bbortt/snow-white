/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.CONFORMING_RESPONSE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
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
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageCalculationCoordinatorUnitTest {

  @Mock
  private OpenApiCoverageCalculator openApiCoverageCalculatorMock;

  private static OpenTelemetryData harnessSpan() {
    return new OpenTelemetryData(
      "span-harness",
      "trace-harness",
      JsonMapper.shared().createObjectNode(),
      null,
      CONFORMING_RESPONSE
    );
  }

  private static OpenTelemetryData applicationSpan() {
    return new OpenTelemetryData(
      "span-application",
      "trace-application",
      JsonMapper.shared().createObjectNode()
    );
  }

  @Nested
  class CalculateTest {

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
        .calculate(
          pathToOpenAPIOperationMap,
          pathToTelemetryMap,
          WaiverContext.none()
        );

      var fixture = new OpenApiCoverageCalculationCoordinator(
        singletonList(openApiCoverageCalculatorMock)
      );
      Set<OpenApiTestResult> result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap,
        WaiverContext.none()
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
        pathToTelemetryMap,
        WaiverContext.none()
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
        pathToTelemetryMap,
        WaiverContext.none()
      );

      assertThat(result).isEmpty();

      verifyNoInteractions(openApiCoverageCalculatorMock);
    }

    /**
     * The window a harness reported verdicts into, with no span of the application's own: the same
     * window without a producer configured carries nothing at all and publishes nothing, so a
     * verdict span may not be what makes every criterion that judges reach publish 0%.
     */
    @Test
    @VerifiesCon(
      ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT
    )
    void shouldReturnEmptySet_whenOnlyAHarnessExportedSpans() {
      pathToOpenAPIOperationMap.put("GET_/foo", mock(Operation.class));
      pathToTelemetryMap.put("GET_/foo", singletonList(harnessSpan()));

      var fixture = new OpenApiCoverageCalculationCoordinator(
        singletonList(openApiCoverageCalculatorMock)
      );

      Set<OpenApiTestResult> result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap,
        WaiverContext.none()
      );

      assertThat(result).isEmpty();

      verifyNoInteractions(openApiCoverageCalculatorMock);
    }

    @Test
    void shouldInvokeEachCalculator_whenTheApplicationContributedASpan() {
      pathToOpenAPIOperationMap.put("GET_/foo", mock(Operation.class));
      pathToTelemetryMap.put(
        "GET_/foo",
        List.of(harnessSpan(), applicationSpan())
      );

      var openApiCriteriaResult = new OpenApiTestResult(
        PATH_COVERAGE,
        BigDecimal.ONE,
        Duration.ofSeconds(1),
        "additionalInformation"
      );

      doReturn(openApiCriteriaResult)
        .when(openApiCoverageCalculatorMock)
        .calculate(
          pathToOpenAPIOperationMap,
          pathToTelemetryMap,
          WaiverContext.none()
        );

      var fixture = new OpenApiCoverageCalculationCoordinator(
        singletonList(openApiCoverageCalculatorMock)
      );

      assertThat(
        fixture.calculate(
          pathToOpenAPIOperationMap,
          pathToTelemetryMap,
          WaiverContext.none()
        )
      ).containsExactly(openApiCriteriaResult);
    }

    @Test
    void shouldReturnEmptySet_withoutAnyOpenApiCoverageCalculator() {
      var fixture = new OpenApiCoverageCalculationCoordinator(emptyList());

      Set<OpenApiTestResult> result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap,
        WaiverContext.none()
      );

      assertThat(result).isEmpty();
    }
  }
}

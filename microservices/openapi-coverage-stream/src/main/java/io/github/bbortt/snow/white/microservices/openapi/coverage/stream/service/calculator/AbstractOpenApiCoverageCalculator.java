/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getStartedStopWatch;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculator;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

abstract class AbstractOpenApiCoverageCalculator
  implements OpenApiCoverageCalculator {

  @Override
  public boolean accepts(OpenApiCriteria openApiCriteria) {
    return getSupportedOpenApiCriteria().equals(openApiCriteria);
  }

  @Override
  public OpenApiTestResult calculate(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var stopWatch = getStartedStopWatch();

    var coverageCalculationResult = calculateCoverage(
      pathToOpenAPIOperationMap,
      pathToTelemetryMap
    );

    return new OpenApiTestResult(
      getSupportedOpenApiCriteria(),
      coverageCalculationResult.coverage(),
      stopWatch.getDuration(),
      coverageCalculationResult.additionalInformation()
    );
  }

  protected abstract @NonNull OpenApiCriteria getSupportedOpenApiCriteria();

  protected abstract @NonNull CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  );

  public record CoverageCalculationResult(
    BigDecimal coverage,
    @Nullable String additionalInformation
  ) {}
}

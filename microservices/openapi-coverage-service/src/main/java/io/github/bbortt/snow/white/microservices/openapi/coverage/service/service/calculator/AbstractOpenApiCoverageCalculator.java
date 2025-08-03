/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator.CalculatorUtils.getStartedStopWatch;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverageCalculator;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

  protected abstract @Nonnull OpenApiCriteria getSupportedOpenApiCriteria();

  protected abstract @Nonnull CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  );

  public record CoverageCalculationResult(
    BigDecimal coverage,
    @Nullable String additionalInformation
  ) {}
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.sort;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria: Each HTTP method (`GET`, `POST`, `PUT`, `DELETE`, etc.) for each path has been tested.
 *
 * @see OpenApiCriteria#ERROR_RESPONSE_CODE_COVERAGE
 */
@Slf4j
@Component
public class MethodCoverageCalculator
  extends AbstractOpenApiCoverageCalculator {

  @Override
  protected @NotNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return HTTP_METHOD_COVERAGE;
  }

  @Override
  public CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var coveredPaths = new AtomicInteger(0);
    var uncoveredPaths = new ArrayList<String>();

    for (String operationKey : pathToOpenAPIOperationMap.keySet()) {
      if (pathToTelemetryMap.containsKey(operationKey)) {
        logger.trace("Path covered: {}", operationKey);
        coveredPaths.incrementAndGet();
      } else {
        logger.trace("Path not covered: {}", operationKey);
        uncoveredPaths.add(operationKey);
      }
    }

    var pathCoverage = calculatePercentage(
      coveredPaths.get(),
      pathToOpenAPIOperationMap.size()
    );

    return new CoverageCalculationResult(
      pathCoverage,
      getAdditionalInformationOrNull(uncoveredPaths)
    );
  }

  private static @Nullable String getAdditionalInformationOrNull(
    @NotNull ArrayList<String> uncoveredPaths
  ) {
    if (uncoveredPaths.isEmpty()) {
      return null;
    }

    sort(uncoveredPaths);

    return format(
      "The following paths are uncovered: `%s`",
      join("`, `", uncoveredPaths)
    );
  }
}

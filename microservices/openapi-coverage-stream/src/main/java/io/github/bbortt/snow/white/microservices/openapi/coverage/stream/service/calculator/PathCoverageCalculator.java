/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria: Every path defined in the OpenAPI specification has been called.
 * This is a subset of `HTTP_METHOD_COVERAGE`.
 *
 * @see OpenApiCriteria#PATH_COVERAGE
 */
@Slf4j
@Component
public class PathCoverageCalculator extends AbstractOpenApiCoverageCalculator {

  @Override
  protected @NotNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return PATH_COVERAGE;
  }

  @Override
  public CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var availableResources = pathToOpenAPIOperationMap
      .keySet()
      .stream()
      .map(OperationKeyCalculator::toPath)
      .collect(toSet());
    var endpointList = pathToTelemetryMap
      .keySet()
      .stream()
      .map(OperationKeyCalculator::toPath)
      .toList();

    var coveredResources = new ArrayList<String>();
    var uncoveredResources = new ArrayList<String>();

    for (String endpoint : availableResources) {
      if (
        coveredResources.contains(endpoint) ||
        uncoveredResources.contains(endpoint)
      ) {
        continue;
      }

      if (endpointList.contains(endpoint)) {
        logger.trace("Resource covered: {}", endpoint);
        coveredResources.add(endpoint);
      } else {
        logger.trace("Resource not covered: {}", endpoint);
        uncoveredResources.add(endpoint);
      }
    }

    var pathCoverage = calculatePercentage(
      coveredResources.size(),
      availableResources.size()
    );

    return new CoverageCalculationResult(
      pathCoverage,
      getAdditionalInformationOrNull(uncoveredResources)
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
      "The following resources (ignoring request methods) are uncovered: `%s`",
      join("`, `", uncoveredPaths)
    );
  }
}

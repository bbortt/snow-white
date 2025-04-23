/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator.CalculatorUtils.getStartedStopWatch;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator.MathUtils.calculatePercentage;
import static java.lang.String.format;
import static java.lang.String.join;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverageCalculator;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MethodCoverageCalculator implements OpenApiCoverageCalculator {

  @Override
  public boolean accepts(OpenApiCriteria openApiCriteria) {
    return HTTP_METHOD_COVERAGE.equals(openApiCriteria);
  }

  @Override
  public OpenApiCriterionResult calculate(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var stopWatch = getStartedStopWatch();

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

    return new OpenApiCriterionResult(
      HTTP_METHOD_COVERAGE,
      pathCoverage,
      stopWatch.getDuration(),
      getAdditionalInformationOrNull(uncoveredPaths)
    );
  }

  private static String getAdditionalInformationOrNull(
    @NotNull ArrayList<String> uncoveredPaths
  ) {
    if (uncoveredPaths.isEmpty()) {
      return null;
    }

    return format(
      "The following paths are uncovered: %s",
      join(", ", uncoveredPaths)
    );
  }
}

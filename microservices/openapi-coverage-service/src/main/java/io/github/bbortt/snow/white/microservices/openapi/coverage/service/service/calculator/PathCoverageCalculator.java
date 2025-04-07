package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator.MathUtils.calculatePercentage;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverageCalculator;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria: Every path defined in the OpenAPI specification has been called.
 *
 * @see OpenApiCriteria#PATH_COVERAGE
 */
@Slf4j
@Component
public class PathCoverageCalculator implements OpenApiCoverageCalculator {

  @Override
  public boolean accepts(OpenApiCriteria openApiCriteria) {
    return PATH_COVERAGE.equals(openApiCriteria);
  }

  @Override
  public OpenApiCriterionResult calculate(
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
      } else if (endpointList.contains(endpoint)) {
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

    return new OpenApiCriterionResult(
      PATH_COVERAGE,
      pathCoverage,
      getAdditionalInformationOrNull(uncoveredResources)
    );
  }

  private static String getAdditionalInformationOrNull(
    @NotNull ArrayList<String> uncoveredPaths
  ) {
    if (uncoveredPaths.isEmpty()) {
      return null;
    }

    return format(
      "The following resources (ignoring request methods) are uncovered: %s",
      join(", ", uncoveredPaths)
    );
  }
}

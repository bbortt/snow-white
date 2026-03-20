/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each parameter (in path, query) has been tested with valid values.
 *
 * @see OpenApiCriteria#PARAMETER_COVERAGE
 */
@Slf4j
@Component
public class ParameterCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return PARAMETER_COVERAGE;
  }

  @Override
  protected @NonNull CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var coveredParameters = new AtomicInteger(0);
    var totalParameters = new AtomicInteger(0);

    var uncoveredParameters = new HashSet<String>();

    for (Map.Entry<
      String,
      Operation
    > entry : pathToOpenAPIOperationMap.entrySet()) {
      String operationKey = entry.getKey();
      Operation operation = entry.getValue();

      if (isEmpty(operation.getParameters())) {
        logger.trace("Operation '{}' has no defined parameters", operationKey);
        continue;
      }

      var parameters = extractParameters(operation);
      if (parameters.isEmpty()) {
        continue;
      }

      totalParameters.addAndGet(parameters.size());

      if (
        !pathToTelemetryMap.containsKey(operationKey) ||
        pathToTelemetryMap.get(operationKey).isEmpty()
      ) {
        logger.trace("No telemetry data for operation: {}", operationKey);
        for (Parameter param : parameters) {
          uncoveredParameters.add(
            format("%s [%s: %s]", operationKey, param.getIn(), param.getName())
          );
        }
        continue;
      }

      var telemetryDataList = pathToTelemetryMap.get(operationKey);

      for (Parameter param : parameters) {
        if (isParameterCovered(telemetryDataList, param, operationKey)) {
          logger.trace(
            "Parameter '{}' ({}) covered in operation '{}'",
            param.getName(),
            param.getIn(),
            operationKey
          );
          coveredParameters.incrementAndGet();
        } else {
          logger.trace(
            "Parameter '{}' ({}) NOT covered in operation '{}'",
            param.getName(),
            param.getIn(),
            operationKey
          );
          uncoveredParameters.add(
            format("%s [%s: %s]", operationKey, param.getIn(), param.getName())
          );
        }
      }
    }

    var parameterCoverage = calculatePercentage(
      coveredParameters.get(),
      totalParameters.get()
    );

    return new CoverageCalculationResult(
      parameterCoverage,
      getAdditionalInformationOrNull(uncoveredParameters)
    );
  }

  /**
   * Extracts parameters to consider for coverage calculation.
   * Override this method in subclasses to filter parameters.
   */
  protected List<Parameter> extractParameters(Operation operation) {
    return operation.getParameters();
  }

  protected boolean isParameterCovered(
    List<OpenTelemetryData> telemetryDataList,
    Parameter param,
    String operationKey
  ) {
    String paramName = param.getName();
    String paramIn = param.getIn();

    for (OpenTelemetryData telemetryData : telemetryDataList) {
      if (isParameterPresent(telemetryData, paramName, paramIn, operationKey)) {
        return true;
      }
    }

    return false;
  }

  private boolean isParameterPresent(
    OpenTelemetryData data,
    String paramName,
    String paramIn,
    String operationKey
  ) {
    if (isNull(data.attributes())) {
      return false;
    }

    return switch (paramIn) {
      case "query" -> isQueryParameterPresent(data, paramName);
      case "path" -> isPathParameterPresent(operationKey, paramName);
      case "header" -> isHeaderParameterPresent(data, paramName);
      default -> {
        logger.trace(
          "Unsupported parameter location '{}' for parameter '{}'",
          paramIn,
          paramName
        );
        yield false;
      }
    };
  }

  private boolean isQueryParameterPresent(
    OpenTelemetryData data,
    String paramName
  ) {
    if (!data.attributes().has(URL_QUERY.getKey())) {
      return false;
    }

    String queryString = data.attributes().get(URL_QUERY.getKey()).asString();
    return (
      nonNull(queryString) &&
      (queryString.contains(paramName + "=") ||
        queryString.contains(paramName + "&") ||
        queryString.endsWith(paramName))
    );
  }

  /**
   * Path parameters are implicitly covered if the telemetry data exists for the operation,
   * as the path template was matched.
   */
  private boolean isPathParameterPresent(
    String operationKey,
    String paramName
  ) {
    String path = OperationKeyCalculator.toPath(operationKey);
    return path.contains("{" + paramName + "}");
  }

  private boolean isHeaderParameterPresent(
    OpenTelemetryData data,
    String paramName
  ) {
    String headerKey = "http.request.header." + paramName.toLowerCase();
    return data.attributes().has(headerKey);
  }

  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Set<String> uncoveredParameters
  ) {
    if (uncoveredParameters.isEmpty()) {
      return null;
    }

    var sortedParameters = uncoveredParameters.stream().sorted().toList();

    return format(
      "The following parameters are uncovered: `%s`",
      join("`, `", sortedParameters)
    );
  }
}

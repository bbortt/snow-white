/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.OPERATION_SUCCESS_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each operation has produced at least one successful (2xx) response.
 * <p>
 * This is distinct from {@link OpenApiCriteria#HTTP_METHOD_COVERAGE}, which only checks that
 * an operation was called at all. This calculator additionally verifies that at least one call
 * resulted in a success response, distinguishing "untested" operations from "called but never
 * succeeded" ones.
 *
 * @see OpenApiCriteria#OPERATION_SUCCESS_COVERAGE
 */
@Slf4j
@Component
public class OperationSuccessCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return OPERATION_SUCCESS_COVERAGE;
  }

  @Override
  protected @NonNull CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var successfulOperations = new AtomicInteger(0);
    var unsuccessfulOperations = new ArrayList<String>();

    for (String operationKey : pathToOpenAPIOperationMap.keySet()) {
      var telemetryList = getTelemetryForTemplate(
        pathToTelemetryMap,
        operationKey
      );

      if (hasSuccessfulResponse(telemetryList)) {
        logger.trace(
          "Operation '{}' has at least one 2xx response",
          operationKey
        );
        successfulOperations.incrementAndGet();
      } else {
        logger.trace(
          "Operation '{}' has no 2xx response observed",
          operationKey
        );
        unsuccessfulOperations.add(operationKey);
      }
    }

    var coverage = calculatePercentage(
      successfulOperations.get(),
      pathToOpenAPIOperationMap.size()
    );

    return new CoverageCalculationResult(
      coverage,
      getAdditionalInformationOrNull(unsuccessfulOperations)
    );
  }

  private boolean hasSuccessfulResponse(List<OpenTelemetryData> telemetryList) {
    for (OpenTelemetryData data : telemetryList) {
      if (isNull(data.attributes())) {
        continue;
      }

      if (!data.attributes().has(HTTP_RESPONSE_STATUS_CODE.getKey())) {
        continue;
      }

      String statusCode = data
        .attributes()
        .get(HTTP_RESPONSE_STATUS_CODE.getKey())
        .asText();

      try {
        if (HttpStatusCode.valueOf(parseInt(statusCode)).is2xxSuccessful()) {
          return true;
        }
      } catch (NumberFormatException _) {
        logger.trace(
          "Skipping non-numeric status code '{}' in success check",
          statusCode
        );
      }
    }

    return false;
  }

  private @Nullable String getAdditionalInformationOrNull(
    @NonNull List<String> unsuccessfulOperations
  ) {
    if (unsuccessfulOperations.isEmpty()) {
      return null;
    }

    var sorted = unsuccessfulOperations.stream().sorted().toList();

    return format(
      "The following operations have no successful (2xx) response observed: `%s`",
      join("`, `", sorted)
    );
  }
}

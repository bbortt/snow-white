/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.NO_UNDOCUMENTED_RESPONSE_CODES;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.findOperationForConcreteKey;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.ResponseCodeCoverageCalculator.SINGLE_DIGIT_PATTERN;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
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
 * All response codes (including errors) that occurred must be documented in the OpenAPI specification.
 *
 * @see OpenApiCriteria#NO_UNDOCUMENTED_RESPONSE_CODES
 */
@Slf4j
@Component
public class NoUndocumentedResponseCodesCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return NO_UNDOCUMENTED_RESPONSE_CODES;
  }

  @Override
  protected @NonNull CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var documentedResponseCodes = new AtomicInteger(0);
    var totalObservedResponseCodes = new AtomicInteger(0);

    var undocumentedResponseCodes = new HashSet<String>();

    for (Map.Entry<
      String,
      List<OpenTelemetryData>
    > entry : pathToTelemetryMap.entrySet()) {
      String operationKey = entry.getKey();
      List<OpenTelemetryData> telemetryDataList = entry.getValue();

      if (isEmpty(telemetryDataList)) {
        continue;
      }

      Operation operation = findOperationForConcreteKey(
        pathToOpenAPIOperationMap,
        operationKey
      );
      Set<String> specifiedResponseCodes = extractSpecifiedResponseCodes(
        operation
      );

      Set<String> observedResponseCodes = extractObservedResponseCodes(
        telemetryDataList
      );
      var filteredObservedCodes = filterObservedResponseCodes(
        observedResponseCodes
      );

      for (String observedCode : filteredObservedCodes) {
        totalObservedResponseCodes.incrementAndGet();

        if (isResponseCodeDocumented(observedCode, specifiedResponseCodes)) {
          documentedResponseCodes.incrementAndGet();
          logger.trace(
            "Response code '{}' is documented for operation '{}'",
            observedCode,
            operationKey
          );
        } else {
          logger.trace(
            "Response code '{}' is NOT documented for operation '{}'",
            observedCode,
            operationKey
          );
          undocumentedResponseCodes.add(
            format("%s [%s]", operationKey, observedCode)
          );
        }
      }
    }

    var coverage = calculatePercentage(
      documentedResponseCodes.get(),
      totalObservedResponseCodes.get()
    );

    return new CoverageCalculationResult(
      coverage,
      getAdditionalInformationOrNull(undocumentedResponseCodes)
    );
  }

  private Set<String> extractSpecifiedResponseCodes(
    @Nullable Operation operation
  ) {
    Set<String> specifiedCodes = new HashSet<>();

    if (isNull(operation) || isNull(operation.getResponses())) {
      return specifiedCodes;
    }

    specifiedCodes.addAll(operation.getResponses().keySet());
    return specifiedCodes;
  }

  private Set<String> extractObservedResponseCodes(
    List<OpenTelemetryData> telemetryDataList
  ) {
    Set<String> observedCodes = new HashSet<>();

    for (OpenTelemetryData telemetryData : telemetryDataList) {
      if (
        isNull(telemetryData.attributes()) ||
        !telemetryData.attributes().has(HTTP_RESPONSE_STATUS_CODE.getKey())
      ) {
        continue;
      }

      String statusCode = telemetryData
        .attributes()
        .get(HTTP_RESPONSE_STATUS_CODE.getKey())
        .asString();
      if (nonNull(statusCode)) {
        observedCodes.add(statusCode);
      }
    }

    return observedCodes;
  }

  /**
   * Filters observed response codes for inclusion in calculation.
   * Override in subclasses to filter specific code ranges.
   */
  protected Set<String> filterObservedResponseCodes(
    Set<String> observedResponseCodes
  ) {
    return observedResponseCodes;
  }

  private boolean isResponseCodeDocumented(
    String observedCode,
    Set<String> specifiedCodes
  ) {
    // Direct match
    if (specifiedCodes.contains(observedCode)) {
      return true;
    }

    // Check wildcard patterns (e.g., "2XX", "4XX", "5XX")
    for (String specifiedCode : specifiedCodes) {
      if (SINGLE_DIGIT_PATTERN.matcher(specifiedCode).matches()) {
        char wildcardPrefix = specifiedCode.charAt(0);
        if (observedCode.startsWith(String.valueOf(wildcardPrefix))) {
          return true;
        }
      }
    }

    // Check for "default" catch-all
    return (
      specifiedCodes.contains("default") || specifiedCodes.contains("DEFAULT")
    );
  }

  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Set<String> undocumentedCodes
  ) {
    return getAdditionalInformationOrNull(
      "The following response codes are not documented in the OpenAPI specification: `%s`",
      undocumentedCodes
    );
  }

  protected @Nullable String getAdditionalInformationOrNull(
    String messagePattern,
    @NonNull Set<String> undocumentedCodes
  ) {
    if (undocumentedCodes.isEmpty()) {
      return null;
    }

    var sortedCodes = undocumentedCodes.stream().sorted().toList();

    return format(messagePattern, join("`, `", sortedCodes));
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator.MathUtils.calculatePercentage;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.compile;
import static org.springframework.data.util.Predicates.negate;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria: Each documented response code for each endpoint is tested.
 *
 * @see OpenApiCriteria#RESPONSE_CODE_COVERAGE
 */
@Slf4j
@Component
public class ResponseCodeCoverageCalculator
  extends AbstractOpenApiCoverageCalculator {

  public static final Pattern SINGLE_DIGIT_PATTERN = compile("^\\dXX$");

  @Override
  protected @NotNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return RESPONSE_CODE_COVERAGE;
  }

  @Override
  public CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var coveredErrorCodes = new AtomicInteger(0);
    var totalErrorCodes = new AtomicInteger(0);

    var uncoveredErrorCodes = new HashSet<String>();

    for (Map.Entry<
      String,
      Operation
    > entry : pathToOpenAPIOperationMap.entrySet()) {
      String path = entry.getKey();
      Operation operation = entry.getValue();

      var responseCodes = extractResponseCodes(operation);
      Set<String> observedErrorCodes = extractObservedErrorCodes(
        pathToTelemetryMap.get(path)
      );

      for (ResponseCode responseCode : responseCodes) {
        var errorCode = responseCode.errorCode();
        if (
          observedErrorCodes
            .stream()
            .anyMatch(responseCode.errorCodePattern().asPredicate())
        ) {
          coveredErrorCodes.incrementAndGet();
          logger.trace("Error code {} is covered for path {}", errorCode, path);
        } else {
          var pathErrorCodeKey = format("%s [%s]", path, errorCode);
          uncoveredErrorCodes.add(pathErrorCodeKey);

          logger.trace(
            "Error code {} is NOT covered for path {}",
            errorCode,
            path
          );
        }
      }

      totalErrorCodes.getAndAdd(responseCodes.size());
    }

    var errorCodesCoverage = calculatePercentage(
      coveredErrorCodes.get(),
      totalErrorCodes.get()
    );

    return new CoverageCalculationResult(
      errorCodesCoverage,
      getAdditionalInformationOrNull(uncoveredErrorCodes)
    );
  }

  protected Set<ResponseCode> extractResponseCodes(Operation operation) {
    Set<ResponseCode> responseCodes = new HashSet<>();

    if (isNull(operation.getResponses())) {
      return responseCodes;
    }

    for (Map.Entry<String, ApiResponse> responseEntry : operation
      .getResponses()
      .entrySet()) {
      String statusCode = responseEntry.getKey();

      if (isResponseCodePattern(statusCode)) {
        responseCodes.add(
          new ResponseCode(
            statusCode,
            compile(format("^%s\\d\\d$", statusCode.charAt(0)))
          )
        );
      } else {
        responseCodes.add(
          new ResponseCode(statusCode, compile(format("^%s$", statusCode)))
        );
      }
    }

    return responseCodes;
  }

  private Set<String> extractObservedErrorCodes(
    @Nullable List<OpenTelemetryData> telemetryDataList
  ) {
    Set<String> observedCodes = new HashSet<>();

    if (isEmpty(telemetryDataList)) {
      return observedCodes;
    }

    for (OpenTelemetryData telemetryData : telemetryDataList) {
      var statusCode = extractStatusCodeFromAttributes(telemetryData);
      if (includeObservedResponseCodeInCalculation(statusCode)) {
        observedCodes.add(statusCode);
      }
    }

    return observedCodes;
  }

  protected boolean includeObservedResponseCodeInCalculation(
    @Nullable String statusCode
  ) {
    return nonNull(statusCode);
  }

  private static @Nullable String extractStatusCodeFromAttributes(
    OpenTelemetryData telemetryData
  ) {
    if (isNull(telemetryData.attributes())) {
      return null;
    }

    var attributes = telemetryData.attributes();
    if (attributes.has(HTTP_RESPONSE_STATUS_CODE.getKey())) {
      return attributes.get(HTTP_RESPONSE_STATUS_CODE.getKey()).asText();
    }

    logger.debug(
      "No HTTP status code found in telemetry attributes for span {}",
      telemetryData.spanId()
    );

    return null;
  }

  /**
   * Detects OpenAPI response code patterns like "4XX", "5XX", "default".
   */
  protected static boolean isResponseCodePattern(@Nullable String statusCode) {
    return (
      nonNull(statusCode) &&
      !statusCode.isEmpty() &&
      SINGLE_DIGIT_PATTERN.matcher(statusCode).matches()
    );
  }

  protected @Nullable String getAdditionalInformationOrNull(
    @NotNull Set<String> uncoveredResponseCodes
  ) {
    return getAdditionalInformationOrNull(
      "The following response codes in paths are uncovered: %s",
      uncoveredResponseCodes
    );
  }

  protected @Nullable String getAdditionalInformationOrNull(
    String infoMessagePattern,
    @NotNull Set<String> uncoveredResponseCodes
  ) {
    if (uncoveredResponseCodes.isEmpty()) {
      return null;
    }

    var codes = uncoveredResponseCodes
      .stream()
      .filter(negate(ResponseCodeCoverageCalculator::isDefaultCodePattern))
      .toList();

    var additionalInformationBuilder = new StringBuilder();

    if (!codes.isEmpty()) {
      additionalInformationBuilder.append(
        format(infoMessagePattern, join(", ", codes.stream().sorted().toList()))
      );
    }

    var defaultCodes = uncoveredResponseCodes
      .stream()
      .filter(ResponseCodeCoverageCalculator::isDefaultCodePattern)
      .toList();

    if (!codes.isEmpty() && !defaultCodes.isEmpty()) {
      additionalInformationBuilder.append(lineSeparator());
    }

    if (!defaultCodes.isEmpty()) {
      additionalInformationBuilder.append(
        format(
          "The following default response codes in paths were being ignored for the calculation: %s",
          join(", ", defaultCodes.stream().sorted().toList())
        )
      );
    }

    return additionalInformationBuilder.toString();
  }

  private static boolean isDefaultCodePattern(String errorCode) {
    return errorCode.endsWith("[default]");
  }

  protected record ResponseCode(String errorCode, Pattern errorCodePattern) {}
}

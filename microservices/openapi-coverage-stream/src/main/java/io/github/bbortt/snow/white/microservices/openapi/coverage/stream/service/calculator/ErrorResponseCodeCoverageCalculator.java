/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.regex.Pattern.compile;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria: Each documented error response code for each endpoint is tested.
 * This is a subset of `RESPONSE_CODE_COVERAGE`.
 *
 * @see OpenApiCriteria#ERROR_RESPONSE_CODE_COVERAGE
 */
@Slf4j
@Component
public class ErrorResponseCodeCoverageCalculator
  extends ResponseCodeCoverageCalculator {

  @Override
  protected @NotNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return ERROR_RESPONSE_CODE_COVERAGE;
  }

  @Override
  protected Set<ResponseCode> extractResponseCodes(Operation operation) {
    Set<ResponseCode> errorResponseCodes = new HashSet<>();

    if (isNull(operation.getResponses())) {
      return errorResponseCodes;
    }

    for (Map.Entry<String, ApiResponse> responseEntry : operation
      .getResponses()
      .entrySet()) {
      String statusCode = responseEntry.getKey();

      var isErrorResponseCode = includeObservedResponseCodeInCalculation(
        statusCode
      );
      if (isErrorResponseCode && isResponseCodePattern(statusCode)) {
        errorResponseCodes.add(
          new ResponseCode(
            statusCode,
            compile(format("^%s\\d\\d$", statusCode.charAt(0)))
          )
        );
      } else if (isErrorResponseCode) {
        errorResponseCodes.add(
          new ResponseCode(statusCode, compile(format("^%s$", statusCode)))
        );
      }
    }

    return errorResponseCodes;
  }

  /**
   * Determines if a status code represents an error response (4xx or 5xx).
   * Also handles OpenAPI patterns like "4XX", "5XX", "default".
   */
  @Override
  protected boolean includeObservedResponseCodeInCalculation(
    @Nullable String statusCode
  ) {
    if (isNull(statusCode)) {
      return false;
    }

    // Handle exact numeric codes
    try {
      int code = parseInt(statusCode);
      return code >= 400 && code <= 599;
    } catch (NumberFormatException e) {
      // Handle pattern codes like "4XX", "5XX", "default"
      String upperCode = statusCode.toUpperCase();
      return (
        upperCode.equals("4XX") ||
        upperCode.equals("5XX") ||
        upperCode.equals("DEFAULT") ||
        upperCode.startsWith("4") ||
        upperCode.startsWith("5")
      );
    }
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NotNull Set<String> uncoveredErrorCodes
  ) {
    return super.getAdditionalInformationOrNull(
      "The following error codes in paths are uncovered: `%s`",
      uncoveredErrorCodes
    );
  }
}

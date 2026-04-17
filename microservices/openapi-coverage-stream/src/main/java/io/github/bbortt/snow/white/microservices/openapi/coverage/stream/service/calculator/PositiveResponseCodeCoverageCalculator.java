/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.POSITIVE_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.HttpStatusCodeUtils.isPositiveHttpStatusCode;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each documented positive (non-error) response code for each endpoint is tested.
 * This is a subset of {@link OpenApiCriteria#RESPONSE_CODE_COVERAGE}.
 *
 * @see OpenApiCriteria#POSITIVE_RESPONSE_CODE_COVERAGE
 */
@Slf4j
@Component
public class PositiveResponseCodeCoverageCalculator
  extends ResponseCodeCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return POSITIVE_RESPONSE_CODE_COVERAGE;
  }

  @Override
  protected Set<ResponseCode> extractResponseCodes(Operation operation) {
    Set<ResponseCode> positiveResponseCodes = new HashSet<>();

    if (operation.getResponses() == null) {
      return positiveResponseCodes;
    }

    for (Map.Entry<String, ApiResponse> responseEntry : operation
      .getResponses()
      .entrySet()) {
      String statusCode = responseEntry.getKey();

      if (!isPositiveHttpStatusCode(statusCode)) {
        continue;
      }

      if (isResponseCodePattern(statusCode)) {
        positiveResponseCodes.add(
          new ResponseCode(
            statusCode,
            compile(format("^%s\\d\\d$", statusCode.charAt(0)))
          )
        );
      } else {
        positiveResponseCodes.add(
          new ResponseCode(statusCode, compile(format("^%s$", statusCode)))
        );
      }
    }

    return positiveResponseCodes;
  }

  @Override
  protected boolean includeObservedResponseCodeInCalculation(
    @Nullable String statusCode
  ) {
    return statusCode != null && isPositiveHttpStatusCode(statusCode);
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Set<String> uncoveredPositiveCodes
  ) {
    return super.getAdditionalInformationOrNull(
      "The following positive response codes in paths are uncovered: `%s`",
      uncoveredPositiveCodes
    );
  }
}

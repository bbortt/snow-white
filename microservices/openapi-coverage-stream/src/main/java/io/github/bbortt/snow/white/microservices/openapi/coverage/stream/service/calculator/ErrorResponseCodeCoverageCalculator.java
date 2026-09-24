/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static java.lang.Integer.parseInt;
import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ApiTestFinding;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each documented error response code for each endpoint is tested.
 * This is a subset of {@link OpenApiCoverageCriteria#RESPONSE_CODE_COVERAGE}.
 *
 * @see OpenApiCoverageCriteria#ERROR_RESPONSE_CODE_COVERAGE
 */
@Slf4j
@Component
public class ErrorResponseCodeCoverageCalculator
  extends ResponseCodeCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return ERROR_RESPONSE_CODE_COVERAGE;
  }

  /**
   * Determines if a status code represents an error response (4xx or 5xx).
   * Also handles OpenAPI patterns like "4XX", "5XX", "default".
   * A documented positive entry is therefore a target this criterion does not judge, rather than
   * one it never saw.
   */
  @RealizesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
  @Override
  protected boolean judgesResponseCode(@Nullable String statusCode) {
    if (isNull(statusCode)) {
      return false;
    }

    // Handle exact numeric codes
    try {
      int code = parseInt(statusCode);
      return code >= 400 && code <= 599;
    } catch (NumberFormatException _) {
      // Handle pattern codes like "4XX", "5XX", "default"
      String upperCode = statusCode.toUpperCase(ROOT);
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
    @NonNull Calculation calculation
  ) {
    return super.getAdditionalInformationOrNull(
      "The following error codes in paths are uncovered: `%s`",
      calculation.findings()
    );
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.POSITIVE_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.HttpStatusCodeUtils.isPositiveHttpStatusCode;
import static java.util.Objects.nonNull;

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
 * Each documented positive (non-error) response code for each endpoint is tested.
 * This is a subset of {@link OpenApiCoverageCriteria#RESPONSE_CODE_COVERAGE}.
 *
 * @see OpenApiCoverageCriteria#POSITIVE_RESPONSE_CODE_COVERAGE
 */
@Slf4j
@Component
public class PositiveResponseCodeCoverageCalculator
  extends ResponseCodeCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return POSITIVE_RESPONSE_CODE_COVERAGE;
  }

  /**
   * Determines if a status code represents a positive response (1xx to 3xx), which excludes
   * {@code default} — never in that range. A documented error entry, and a documented
   * {@code default}, are therefore targets this criterion does not judge, rather than ones it
   * never saw.
   */
  @RealizesSw(
    SwTraceables.SW_002_RESPONSE_CODE_COVERAGE_TREATS_DEFAULT_AS_WILDCARD
  )
  @RealizesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
  @Override
  protected boolean judgesResponseCode(@Nullable String statusCode) {
    return nonNull(statusCode) && isPositiveHttpStatusCode(statusCode);
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull List<ApiTestFinding> findings
  ) {
    return super.getAdditionalInformationOrNull(
      "The following positive response codes in paths are uncovered: `%s`",
      findings
    );
  }
}

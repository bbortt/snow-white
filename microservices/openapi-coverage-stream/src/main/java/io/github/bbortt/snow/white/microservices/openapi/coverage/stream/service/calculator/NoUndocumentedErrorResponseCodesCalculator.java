/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_ERROR_RESPONSE_CODES;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.HttpStatusCodeUtils.isErrorHttpStatusCode;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * All error response codes that occurred must be documented in the OpenAPI specification.
 * This is a subset of {@link OpenApiCoverageCriteria#NO_UNDOCUMENTED_RESPONSE_CODES}.
 *
 * @see OpenApiCoverageCriteria#NO_UNDOCUMENTED_ERROR_RESPONSE_CODES
 */
@Slf4j
@Component
public class NoUndocumentedErrorResponseCodesCalculator
  extends NoUndocumentedResponseCodesCalculator
{

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return NO_UNDOCUMENTED_ERROR_RESPONSE_CODES;
  }

  @RealizesSw(SwTraceables.SW_003_UNDOCUMENTED_RESPONSE_CODE_DETECTION)
  @Override
  protected boolean judgesObservedResponseCode(
    @NonNull String observedResponseCode
  ) {
    return isErrorHttpStatusCode(observedResponseCode);
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    return super.getAdditionalInformationOrNull(
      "The following observed error response codes are not documented in the OpenAPI specification: `%s`",
      calculation.findings()
    );
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_PARAMETER_COVERAGE;
import static java.lang.Boolean.TRUE;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each required parameter (in path, query) has been tested with valid values.
 * This is a subset of {@link OpenApiCoverageCriteria#PARAMETER_COVERAGE}.
 *
 * @see OpenApiCoverageCriteria#REQUIRED_PARAMETER_COVERAGE
 */
@Slf4j
@Component
public class RequiredParameterCoverageCalculator
  extends ParameterCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return REQUIRED_PARAMETER_COVERAGE;
  }

  /**
   * A declared optional parameter is therefore a target this criterion does not judge, rather than
   * one it never saw.
   */
  @RealizesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
  @Override
  protected boolean judgesParameter(@NonNull Parameter parameter) {
    return TRUE.equals(parameter.getRequired());
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    return super.getAdditionalInformationOrNull(
      "The following required parameters are uncovered: `%s`",
      calculation
    );
  }
}

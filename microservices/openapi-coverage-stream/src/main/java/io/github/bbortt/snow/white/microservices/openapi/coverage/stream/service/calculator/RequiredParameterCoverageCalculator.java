/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_PARAMETER_COVERAGE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.lang.String.join;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each required parameter (in path, query) has been tested with valid values.
 * This is a subset of {@link OpenApiCriteria#PARAMETER_COVERAGE}.
 *
 * @see OpenApiCriteria#REQUIRED_PARAMETER_COVERAGE
 */
@Slf4j
@Component
public class RequiredParameterCoverageCalculator
  extends ParameterCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return REQUIRED_PARAMETER_COVERAGE;
  }

  @Override
  protected List<Parameter> extractParameters(Operation operation) {
    return operation
      .getParameters()
      .stream()
      .filter(param -> TRUE.equals(param.getRequired()))
      .toList();
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Set<String> uncoveredParameters
  ) {
    if (uncoveredParameters.isEmpty()) {
      return null;
    }

    var sortedParameters = uncoveredParameters.stream().sorted().toList();

    return format(
      "The following required parameters are uncovered: `%s`",
      join("`, `", sortedParameters)
    );
  }
}

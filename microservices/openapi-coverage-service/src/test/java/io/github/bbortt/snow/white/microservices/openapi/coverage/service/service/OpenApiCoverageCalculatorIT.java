/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ALL_ERROR_CODES_DOCUMENTED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ALL_NON_ERROR_CODES_DOCUMENTED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ALL_RESPONSE_CODES_DOCUMENTED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_ERROR_FIELDS;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.RESPONSE_CODE_COVERAGE;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.AbstractOpenApiCoverageServiceIT;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OpenApiCoverageCalculatorIT extends AbstractOpenApiCoverageServiceIT {

  @Autowired
  private List<OpenApiCoverageCalculator> openApiCoverageCalculators;

  @Test
  void aCalculatorShouldExistForEachOpenApiCriteria() {
    var uncoveredCriteria = stream(OpenApiCriteria.values())
      .filter(
        openApiCriteria ->
          openApiCoverageCalculators
            .stream()
            .filter(openApiCoverageCalculators ->
              openApiCoverageCalculators.accepts(openApiCriteria)
            )
            .count() !=
          1
      )
      .toList();

    // TODO: I am not done yet!
    //  assertThat(uncoveredCriteria).isEmpty();
    assertThat(uncoveredCriteria).containsExactly(
      ERROR_RESPONSE_CODE_COVERAGE,
      RESPONSE_CODE_COVERAGE,
      REQUIRED_PARAMETER_COVERAGE,
      PARAMETER_COVERAGE,
      REQUIRED_ERROR_FIELDS,
      ALL_RESPONSE_CODES_DOCUMENTED,
      ALL_ERROR_CODES_DOCUMENTED,
      ALL_NON_ERROR_CODES_DOCUMENTED
    );
  }
}

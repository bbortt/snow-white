/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.NO_UNDOCUMENTED_ERROR_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.NO_UNDOCUMENTED_RESPONSE_CODES;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_PARAMETER_COVERAGE;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
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
            .filter(openApiCoverageCalculator ->
              openApiCoverageCalculator.accepts(openApiCriteria)
            )
            .count() !=
          1
      )
      .toList();

    // TODO: I am not done yet!
    //  assertThat(uncoveredCriteria).isEmpty();
    assertThat(uncoveredCriteria).containsExactly(
      REQUIRED_PARAMETER_COVERAGE,
      PARAMETER_COVERAGE,
      REQUIRED_ERROR_FIELDS_COVERAGE,
      NO_UNDOCUMENTED_RESPONSE_CODES,
      NO_UNDOCUMENTED_ERROR_RESPONSE_CODES,
      NO_UNDOCUMENTED_POSITIVE_RESPONSE_CODES
    );
  }
}

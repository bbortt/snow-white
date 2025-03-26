package io.github.bbortt.snow.white.microservices.report.coordination.service.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenApiCoverageTest {

  @Nested
  class Passed {

    @Test
    void shouldReturnTrue_whenAllBooleanFieldsAreTrue() {
      var coverage = OpenApiCoverage.builder()
        .pathCoverageMet(true)
        .responseCodeCoverageMet(true)
        .errorResponseCoverageMet(true)
        .requiredParameterCoverageMet(true)
        .headerParameterCoverageMet(true)
        .queryParameterCoverageMet(true)
        .requestBodySchemaCoverageMet(true)
        .contentTypeCoverageMet(true)
        .build();

      boolean result = coverage.passed();

      assertTrue(result);
    }

    @Test
    void shouldReturnFalse_whenAnyBooleanFieldIsFalse() {
      OpenApiCoverage coverage = OpenApiCoverage.builder()
        .pathCoverageMet(true)
        .responseCodeCoverageMet(true)
        .errorResponseCoverageMet(true)
        .requiredParameterCoverageMet(true)
        .headerParameterCoverageMet(true)
        .queryParameterCoverageMet(true)
        .requestBodySchemaCoverageMet(false) // This one is false
        .contentTypeCoverageMet(true)
        .build();

      boolean result = coverage.passed();

      assertFalse(result);
    }

    @Test
    void shouldReturnFalse_whenAnyBooleanFieldIsNull() {
      OpenApiCoverage coverage = OpenApiCoverage.builder()
        .pathCoverageMet(true)
        .responseCodeCoverageMet(true)
        .errorResponseCoverageMet(true)
        .requiredParameterCoverageMet(true)
        .headerParameterCoverageMet(true)
        .queryParameterCoverageMet(null) // This one is null
        .requestBodySchemaCoverageMet(true)
        .contentTypeCoverageMet(true)
        .build();

      boolean result = coverage.passed();

      assertFalse(result);
    }
  }
}

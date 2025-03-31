package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenApiCoverageReportTest {

  @Nested
  class Passed {

    @Test
    void shouldReturnTrue_whenAllBooleanFieldsAreTrue() {
      var coverage = OpenApiCoverageReport.builder()
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
      OpenApiCoverageReport coverage = OpenApiCoverageReport.builder()
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
      OpenApiCoverageReport coverage = OpenApiCoverageReport.builder()
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

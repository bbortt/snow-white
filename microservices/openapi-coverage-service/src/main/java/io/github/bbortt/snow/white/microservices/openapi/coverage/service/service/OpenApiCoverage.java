package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
final class OpenApiCoverage {

  private BigDecimal pathCoverage;
  private BigDecimal responseCodeCoverage;
  private BigDecimal requiredParameterCoverage;
  private BigDecimal queryParameterCoverage;
  private BigDecimal headerParameterCoverage;
  private BigDecimal requestBodySchemaCoverage;
  private Boolean errorResponseCoveredAtLeastOnce;
  private BigDecimal errorResponseCoverage;
  private BigDecimal contentTypeCoverage;
}

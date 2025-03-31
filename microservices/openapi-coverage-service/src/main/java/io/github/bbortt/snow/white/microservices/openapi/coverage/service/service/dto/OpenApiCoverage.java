package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto;

import java.math.BigDecimal;

public record OpenApiCoverage(
  BigDecimal pathCoverage,
  BigDecimal responseCodeCoverage,
  BigDecimal requiredParameterCoverage,
  BigDecimal queryParameterCoverage,
  BigDecimal headerParameterCoverage,
  BigDecimal requestBodySchemaCoverage,
  Boolean errorResponseCoveredAtLeastOnce,
  BigDecimal errorResponseCoverage,
  BigDecimal contentTypeCoverage
) {}

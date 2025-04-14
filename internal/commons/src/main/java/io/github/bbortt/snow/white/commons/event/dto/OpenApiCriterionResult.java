package io.github.bbortt.snow.white.commons.event.dto;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import jakarta.annotation.Nullable;
import java.math.BigDecimal;

public record OpenApiCriterionResult(
  OpenApiCriteria openApiCriteria,
  BigDecimal coverage,
  @Nullable String additionalInformation
) {
  public OpenApiCriterionResult(
    OpenApiCriteria openApiCriteria,
    BigDecimal coverage
  ) {
    this(openApiCriteria, coverage, null);
  }
}

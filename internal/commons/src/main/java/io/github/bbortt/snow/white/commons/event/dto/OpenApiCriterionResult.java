package io.github.bbortt.snow.white.commons.event.dto;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import java.math.BigDecimal;

public record OpenApiCriterionResult(
  OpenApiCriteria openApiCriteria,
  BigDecimal coverage,
  String additionalInformation
) {}

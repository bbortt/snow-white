package io.github.bbortt.snow.white.commons.event;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.With;

@With
@Getter
@RequiredArgsConstructor
public final class OpenApiCoverageResponseEvent {

  private final BigDecimal pathCoverage;
  private final BigDecimal responseCodeCoverage;
  private final BigDecimal requiredParameterCoverage;
  private final BigDecimal queryParameterCoverage;
  private final BigDecimal headerParameterCoverage;
  private final BigDecimal requestBodySchemaCoverage;
  private final Boolean errorResponseCoveredAtLeastOnce;
  private final BigDecimal errorResponseCoverage;
  private final BigDecimal contentTypeCoverage;
}

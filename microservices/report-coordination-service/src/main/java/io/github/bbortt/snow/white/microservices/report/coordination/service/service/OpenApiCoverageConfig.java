package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OpenApiCoverageConfig {

  @Nonnull
  private Boolean includesPathCoverage;

  @Nonnull
  private Boolean includesResponseCodeCoverage;

  @Nonnull
  private Boolean includesErrorResponseCoverage;

  @Nonnull
  private Boolean includesRequiredParameterCoverage;

  @Nonnull
  private Boolean includesHeaderParameterCoverage;

  @Nonnull
  private Boolean includesQueryParameterCoverage;

  @Nonnull
  private Boolean includesRequestBodySchemaCoverage;

  @Nonnull
  private Boolean includesContentTypeCoverage;
}

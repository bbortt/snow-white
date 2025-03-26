package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QualityGateConfig {

  @Nonnull
  private String name;

  private @Nullable String description;

  @Nonnull
  private OpenApiCoverageConfig openApiCoverageConfig;
}

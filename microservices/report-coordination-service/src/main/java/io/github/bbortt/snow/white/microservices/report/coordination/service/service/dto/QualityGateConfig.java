package io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QualityGateConfig {

  @Nonnull
  private final String name;

  @Nullable
  private OpenApiCoverageConfig openApiCoverageConfig;
}

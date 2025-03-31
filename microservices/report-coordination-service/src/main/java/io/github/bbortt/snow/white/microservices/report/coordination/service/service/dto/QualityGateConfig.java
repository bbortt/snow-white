package io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QualityGateConfig {

  @Nonnull
  private String name;

  @Nonnull
  private OpenApiCoverageConfig openApiCoverageConfig;
}

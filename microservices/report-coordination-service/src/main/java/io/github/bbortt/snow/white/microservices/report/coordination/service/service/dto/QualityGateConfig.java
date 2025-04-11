package io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QualityGateConfig {

  @NotEmpty
  private final String name;

  @Nonnull
  private List<String> openapiCriteria;
}

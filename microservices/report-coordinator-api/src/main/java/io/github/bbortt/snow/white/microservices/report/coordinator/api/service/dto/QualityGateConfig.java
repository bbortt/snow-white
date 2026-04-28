/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

@Getter
@AllArgsConstructor
public class QualityGateConfig {

  @NotEmpty
  private final String name;

  @NonNull
  private Set<String> openApiCriteria;

  @NotNull
  @Min(80)
  @Max(100)
  private final Integer minCoveragePercentage;
}

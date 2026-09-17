/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.init;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@NullMarked
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

  private final OpenApiCoverageConfigurationService openApiCoverageConfigurationService;
  private final QualityGateService qualityGateService;

  /**
   * Criteria existence rows must be seeded before predefined gates, every time — predefined-gate
   * seeding fails fast if a referenced criterion row does not yet exist.
   */
  @RealizesArch(ArchTraceables.ARCH_003_IDEMPOTENT_ORDERED_STARTUP_SEEDING)
  @Override
  public void run(String... args) {
    openApiCoverageConfigurationService.initOpenApiCoverageCriteria();
    qualityGateService.initPredefinedQualityGates();
  }
}

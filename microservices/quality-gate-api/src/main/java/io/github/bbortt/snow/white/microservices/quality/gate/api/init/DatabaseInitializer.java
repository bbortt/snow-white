/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.init;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesCon;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.GateOwnershipService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateFileReader;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@NullMarked
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

  private final GateOwnershipService gateOwnershipService;
  private final OpenApiCoverageConfigurationService openApiCoverageConfigurationService;
  private final QualityGateFileReader qualityGateFileReader;
  private final QualityGateService qualityGateService;

  /**
   * Criteria existence rows must be seeded before the gates, every time — gate seeding resolves a
   * referenced criterion by looking its row up, and fails fast if it does not yet exist.
   *
   * <p>The second step's definitions come from the gate file on a file-owned instance and from code
   * otherwise; the step keeps its place in the ordering either way, and criteria seeding is
   * unconditional because the criteria catalog is a catalog of what can be measured, not a gate.
   * Nothing here catches what the file read throws: a configured file that cannot be turned into a
   * gate set has to fail the startup, because the only other option is deleting gates on the
   * strength of a file nobody could read.
   */
  @RealizesArch(ArchTraceables.ARCH_003_IDEMPOTENT_ORDERED_STARTUP_SEEDING)
  @RealizesCon(
    ConTraceables.CON_016_MANAGED_INSTANCE_HAS_EXACTLY_ONE_GATE_WRITER
  )
  @Override
  public void run(String... args) {
    openApiCoverageConfigurationService.initOpenApiCoverageCriteria();

    if (gateOwnershipService.isFileOwned()) {
      qualityGateService.applyQualityGateFile(
        qualityGateFileReader.readQualityGateFile()
      );
    } else {
      qualityGateService.initPredefinedQualityGates();
    }
  }
}

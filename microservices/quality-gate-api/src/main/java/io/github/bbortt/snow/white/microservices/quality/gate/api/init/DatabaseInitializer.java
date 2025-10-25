/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.init;

import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

  private final OpenApiCoverageConfigurationService openApiCoverageConfigurationService;
  private final QualityGateService qualityGateService;

  @Override
  public void run(String... args) {
    openApiCoverageConfigurationService.initOpenApiCriteria();
    qualityGateService.initPredefinedQualityGates();
  }
}

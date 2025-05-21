/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service;

import io.github.bbortt.snow.white.microservices.report.coordination.service.service.OpenApiCriterionService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

  private final OpenApiCriterionService openApiCriterionService;

  @Override
  public void run(String... args) {
    openApiCriterionService.initOpenApiTestCriteria();
  }
}

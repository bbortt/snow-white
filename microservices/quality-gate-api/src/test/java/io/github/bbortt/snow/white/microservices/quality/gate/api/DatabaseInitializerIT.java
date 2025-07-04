/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DatabaseInitializerIT extends AbstractQualityGateApiIT {

  @Autowired
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  @Autowired
  private QualityGateConfigurationRepository qualityGateConfigurationRepository;

  @Test
  void shouldInitOpenApiCriteria() {
    var openApiCoverageConfigurations =
      openApiCoverageConfigurationRepository.findAll();

    assertThat(openApiCoverageConfigurations)
      .map(OpenApiCoverageConfiguration::getName)
      .containsExactlyInAnyOrder(
        stream(OpenApiCriteria.values())
          .map(OpenApiCriteria::name)
          .toArray(String[]::new)
      );
  }

  @Test
  void shouldInitPredefinedQualityGates() {
    var qualityGateConfigurations =
      qualityGateConfigurationRepository.findAll();

    assertThat(qualityGateConfigurations)
      .satisfies(list ->
        assertThat(list)
          .map(QualityGateConfiguration::getName)
          .containsExactlyInAnyOrder(
            "full-feature",
            "basic-coverage",
            "minimal",
            "dry-run"
          )
      )
      .allSatisfy(qualityGateConfiguration ->
        assertThat(qualityGateConfiguration.getIsPredefined()).isTrue()
      );
  }
}

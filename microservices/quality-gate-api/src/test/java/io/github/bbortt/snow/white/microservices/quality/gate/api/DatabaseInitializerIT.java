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
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class DatabaseInitializerIT {

  @Autowired
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  @Test
  void persistsEachEnumValue() {
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
}

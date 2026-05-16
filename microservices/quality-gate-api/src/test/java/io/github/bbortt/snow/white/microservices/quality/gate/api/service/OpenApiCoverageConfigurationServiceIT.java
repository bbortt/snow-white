/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.AbstractQualityGateApiIT;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OpenApiCoverageConfigurationServiceIT extends AbstractQualityGateApiIT {

  @Autowired
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  @Autowired
  private OpenApiCoverageConfigurationService fixture;

  @Nested
  class InitOpenApiCriteriaTest {

    @Test
    void isIdempotent() {
      assertDoesNotThrow(() -> fixture.initOpenApiCoverageCriteria());

      assertThat(openApiCoverageConfigurationRepository.count()).isEqualTo(
        OpenApiCoverageCriteria.values().length
      );
    }
  }
}

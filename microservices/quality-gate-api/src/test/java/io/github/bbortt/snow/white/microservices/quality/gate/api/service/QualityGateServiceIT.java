/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.quality.gate.api.AbstractQualityGateApiIT;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QualityGateServiceIT extends AbstractQualityGateApiIT {

  @Autowired
  private QualityGateConfigurationRepository qualityGateConfigurationRepository;

  @Autowired
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  @Autowired
  private QualityGateService fixture;

  @Nested
  class initPredefinedQualityGatesTest {

    @Test
    void isIdempotent() {
      assertDoesNotThrow(() -> fixture.initPredefinedQualityGates());

      assertThat(qualityGateConfigurationRepository.count()).isEqualTo(4);
    }

    /**
     * Reproduces a gate composition change: a criterion once part of a predefined gate's default
     * set must be un-attached on the next reseed, not just left behind alongside newly attached
     * ones.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_011_FOUR_PREDEFINED_GATES_FIXED_COMPOSITION)
    void removesCriteriaNoLongerPartOfTheDefaultComposition() {
      var minimal = qualityGateConfigurationRepository
        .findByName("minimal")
        .orElseThrow();
      minimal = minimal.withOpenApiCoverageConfiguration(
        openApiCoverageConfigurationRepository
          .findByName(HTTP_METHOD_COVERAGE.name())
          .orElseThrow()
      );
      qualityGateConfigurationRepository.save(minimal);

      fixture.initPredefinedQualityGates();

      var reseededMinimal = qualityGateConfigurationRepository
        .findByName("minimal")
        .orElseThrow();
      assertThat(
        reseededMinimal
          .getOpenApiCoverageConfigurations()
          .stream()
          .map(
            QualityGateOpenApiCoverageMapping::getOpenApiCoverageConfiguration
          )
          .map(OpenApiCoverageConfiguration::getName)
      ).containsExactly(PATH_COVERAGE.name());
    }
  }
}

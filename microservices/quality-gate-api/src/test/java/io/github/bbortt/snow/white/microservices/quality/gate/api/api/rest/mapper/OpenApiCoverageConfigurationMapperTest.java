/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OpenApiCoverageConfigurationMapperTest {

  private OpenApiCoverageConfigurationMapper fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCoverageConfigurationMapperImpl();
  }

  @Nested
  class ToDtos {

    @ParameterizedTest
    @EnumSource(OpenApiCriteria.class)
    void shouldMapAllProperties(OpenApiCriteria openApiCriteria) {
      var openApiCoverageConfiguration = OpenApiCoverageConfiguration.builder()
        .name(openApiCriteria.name())
        .build();

      var openApiCoverageConfigurations = singleton(
        openApiCoverageConfiguration
      );
      assertThat(fixture.toDtos(openApiCoverageConfigurations))
        .isNotNull()
        .hasSize(1)
        .first()
        .satisfies(
          d -> assertThat(d.getId()).isEqualTo(openApiCriteria.name()),
          d -> assertThat(d.getName()).isEqualTo(openApiCriteria.getLabel()),
          d ->
            assertThat(d.getDescription()).isEqualTo(
              openApiCriteria.getDescription()
            )
        );
    }

    @Test
    void shouldMapAllInSet() {
      var pathCoverage = OpenApiCoverageConfiguration.builder()
        .name(PATH_COVERAGE.name())
        .build();
      var httpMethodCoverage = OpenApiCoverageConfiguration.builder()
        .name(HTTP_METHOD_COVERAGE.name())
        .build();

      assertThat(fixture.toDtos(Set.of(pathCoverage, httpMethodCoverage)))
        .isNotNull()
        .hasSize(2);
    }

    @Test
    void shouldThrowException_whenInvalidNameSupplied() {
      var openApiCriteria = "openApiCriteria";
      var openApiCoverageConfiguration = OpenApiCoverageConfiguration.builder()
        .name(openApiCriteria)
        .build();

      var openApiCoverageConfigurations = singleton(
        openApiCoverageConfiguration
      );

      assertThatThrownBy(() -> fixture.toDtos(openApiCoverageConfigurations))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("No enum constant")
        .hasMessageEndingWith(openApiCriteria);
    }
  }
}

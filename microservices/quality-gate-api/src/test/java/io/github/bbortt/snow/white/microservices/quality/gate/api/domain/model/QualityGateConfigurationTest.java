/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class QualityGateConfigurationTest {

  private QualityGateConfiguration fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = QualityGateConfiguration.builder().name("default-gate").build();
  }

  @Nested
  class WithId {

    @Mock
    private QualityGateOpenApiCoverageMapping qualityGateOpenApiCoverageMappingMock;

    @BeforeEach
    void beforeEachSetup() {
      fixture = fixture.withOpenApiCoverageConfigurations(
        Set.of(qualityGateOpenApiCoverageMappingMock)
      );
    }

    @Test
    void shouldPropagateIdToMappings() {
      var id = 1234L;

      var updatedQualityGateConfiguration = fixture.withId(id);

      assertThat(updatedQualityGateConfiguration.getId()).isEqualTo(id);
      verify(qualityGateOpenApiCoverageMappingMock).setQualityGateConfiguration(
        fixture
      );
    }
  }

  @Nested
  class WithOpenApiCoverageConfiguration {

    private OpenApiCoverageConfiguration coverageConfig;

    @BeforeEach
    void beforeEachSetup() {
      coverageConfig = OpenApiCoverageConfiguration.builder()
        .name("first")
        .build();
    }

    @Test
    void shouldAddOpenApiCoverageConfigurationMapping() {
      QualityGateConfiguration result =
        fixture.withOpenApiCoverageConfiguration(coverageConfig);

      assertThat(result.getOpenApiCoverageConfigurations())
        .isNotNull()
        .hasSize(1);

      QualityGateOpenApiCoverageMapping mapping = result
        .getOpenApiCoverageConfigurations()
        .iterator()
        .next();

      assertThat(mapping.getQualityGateConfiguration().getName()).isEqualTo(
        fixture.getName()
      );
    }

    @Test
    void shouldReturnSameInstanceWhenCalled() {
      QualityGateConfiguration modified =
        fixture.withOpenApiCoverageConfiguration(coverageConfig);

      assertSame(modified, fixture); // this will fail — only pass if with-method returns same object
    }

    @Test
    void shouldNotFailWhenAddingMultipleConfigurations() {
      OpenApiCoverageConfiguration coverageConfig2 =
        OpenApiCoverageConfiguration.builder().name("second").build();

      fixture
        .withOpenApiCoverageConfiguration(coverageConfig)
        .withOpenApiCoverageConfiguration(coverageConfig2);

      Set<QualityGateOpenApiCoverageMapping> mappings =
        fixture.getOpenApiCoverageConfigurations();
      assertThat(mappings).hasSize(2);
    }
  }
}

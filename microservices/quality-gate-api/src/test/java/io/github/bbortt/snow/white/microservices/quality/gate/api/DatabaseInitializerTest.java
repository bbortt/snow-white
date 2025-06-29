/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.QualityGateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class DatabaseInitializerTest {

  @Mock
  private OpenApiCoverageConfigurationService openApiCoverageConfigurationServiceMock;

  @Mock
  private QualityGateService qualityGateServiceMock;

  private DatabaseInitializer fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new DatabaseInitializer(
      openApiCoverageConfigurationServiceMock,
      qualityGateServiceMock
    );
  }

  @Nested
  class Run {

    @Test
    void shouldInitiateDatabaseInitialization() {
      fixture.run();

      verify(openApiCoverageConfigurationServiceMock).initOpenApiCriteria();
      verify(qualityGateServiceMock).initPredefinedQualityGates();
    }
  }
}

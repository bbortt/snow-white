/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCoverageConfigurationServiceTest {

  @Mock
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepositoryMock;

  private OpenApiCoverageConfigurationService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCoverageConfigurationService(
      openApiCoverageConfigurationRepositoryMock
    );
  }

  @Nested
  class GetAllOpenapiCoverageConfigurations {

    @Test
    void shouldReturnOpenApiCoverageConfigurations() {
      var openApiCoverageConfiguration = new OpenApiCoverageConfiguration();
      doReturn(singletonList(openApiCoverageConfiguration))
        .when(openApiCoverageConfigurationRepositoryMock)
        .findAll();

      Set<OpenApiCoverageConfiguration> result =
        fixture.getAllOpenapiCoverageConfigurations();

      assertThat(result).containsExactly(openApiCoverageConfiguration);
    }
  }

  @Nested
  class InitOpenApiCriteria {

    @Test
    void shouldAddEachMissingOpenApiCriteriaToDatabase() {
      doReturn(false)
        .when(openApiCoverageConfigurationRepositoryMock)
        .existsByName(anyString());

      fixture.initOpenApiCriteria();

      ArgumentCaptor<
        List<OpenApiCoverageConfiguration>
      > openApiCoverageConfigurationArgumentCaptor = captor();
      verify(openApiCoverageConfigurationRepositoryMock).saveAll(
        openApiCoverageConfigurationArgumentCaptor.capture()
      );

      assertThat(openApiCoverageConfigurationArgumentCaptor.getValue())
        .isNotEmpty()
        .map(OpenApiCoverageConfiguration::getName)
        .containsExactlyInAnyOrder(
          stream(OpenApiCriteria.values())
            .map(OpenApiCriteria::name)
            .toArray(String[]::new)
        );

      verify(
        openApiCoverageConfigurationRepositoryMock,
        times(OpenApiCriteria.values().length)
      ).existsByName(anyString());
    }

    @Test
    void shouldNotAddAnyOpenApiCriteriaToDatabase_whenAllAreAlreadyPresent() {
      doReturn(true)
        .when(openApiCoverageConfigurationRepositoryMock)
        .existsByName(anyString());

      fixture.initOpenApiCriteria();

      verify(
        openApiCoverageConfigurationRepositoryMock,
        times(OpenApiCriteria.values().length)
      ).existsByName(anyString());
      verifyNoMoreInteractions(openApiCoverageConfigurationRepositoryMock);
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.OpenApiCriterionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCriterionServiceTest {

  @Mock
  private OpenApiCriterionRepository openApiCriterionRepositoryMock;

  private OpenApiCriterionService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCriterionService(openApiCriterionRepositoryMock);
  }

  @Nested
  class InitOpenApiTestCriteria {

    @Test
    void shouldAddEachMissingOpenApiTestCriteriaToDatabase() {
      doReturn(false)
        .when(openApiCriterionRepositoryMock)
        .existsByName(anyString());

      fixture.initOpenApiTestCriteria();

      ArgumentCaptor<List<OpenApiTestCriteria>> openApiTestCriteriaCaptor =
        captor();
      verify(openApiCriterionRepositoryMock).saveAll(
        openApiTestCriteriaCaptor.capture()
      );

      assertThat(openApiTestCriteriaCaptor.getValue())
        .isNotEmpty()
        .map(OpenApiTestCriteria::getName)
        .containsExactlyInAnyOrder(
          stream(OpenApiCriteria.values())
            .map(OpenApiCriteria::name)
            .toArray(String[]::new)
        );

      verify(
        openApiCriterionRepositoryMock,
        times(OpenApiCriteria.values().length)
      ).existsByName(anyString());
    }

    @Test
    void shouldNotAddAnyOpenApiTestCriteriaToDatabase_whenAllAreAlreadyPresent() {
      doReturn(true)
        .when(openApiCriterionRepositoryMock)
        .existsByName(anyString());

      fixture.initOpenApiTestCriteria();

      verify(
        openApiCriterionRepositoryMock,
        times(OpenApiCriteria.values().length)
      ).existsByName(anyString());
      verifyNoMoreInteractions(openApiCriterionRepositoryMock);
    }
  }
}

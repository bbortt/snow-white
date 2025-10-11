/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.OpenApiCoverageConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class CriteriaResourceTest {

  @Mock
  private OpenApiCoverageConfigurationMapper openApiCoverageConfigurationMapperMock;

  @Mock
  private OpenApiCoverageConfigurationService openApiCoverageConfigurationServiceMock;

  private CriteriaResource fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new CriteriaResource(
      openApiCoverageConfigurationMapperMock,
      openApiCoverageConfigurationServiceMock
    );
  }

  @Nested
  class ListOpenApiCriteria {

    @Test
    void returnsAllCriteria() {
      var openApiCoverageConfigurations = Set.of(
        new OpenApiCoverageConfiguration()
      );
      doReturn(openApiCoverageConfigurations)
        .when(openApiCoverageConfigurationServiceMock)
        .getAllOpenapiCoverageConfigurations();

      var openApiCriteria = singletonList(new OpenApiCriterion());
      doReturn(openApiCriteria)
        .when(openApiCoverageConfigurationMapperMock)
        .toDtos(openApiCoverageConfigurations);

      var response = fixture.listOpenApiCriteria();

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isEqualTo(openApiCriteria)
        );
    }
  }
}

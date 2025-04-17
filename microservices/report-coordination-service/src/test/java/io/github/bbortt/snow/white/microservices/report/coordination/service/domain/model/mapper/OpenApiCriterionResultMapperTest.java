/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.OpenApiCriterionRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiCriterionResultMapperTest {

  @Mock
  private OpenApiCriterionRepository openApiCriterionRepositoryMock;

  private OpenApiCriterionResultMapper fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCriterionResultMapperImpl();
    fixture.setOpenApiCriterionRepository(openApiCriterionRepositoryMock);
  }

  @Nested
  class OpenApiCriteriaToIncludedInReport {

    @Test
    void shouldAlwaysReturnTrue() {
      assertThat(fixture.openApiCriteriaToIncludedInReport(null))
        .isNotNull()
        .isTrue();
    }
  }

  @Nested
  class OpenApiCriteriaToName {

    @ParameterizedTest
    @EnumSource(OpenApiCriteria.class)
    void shouldExtractName(OpenApiCriteria openApiCriteria) {
      doAnswer(invocationOnMock ->
        Optional.of(
          OpenApiCriterion.builder()
            .name(invocationOnMock.getArgument(0))
            .build()
        )
      )
        .when(openApiCriterionRepositoryMock)
        .findByName(openApiCriteria.name());

      assertThat(fixture.getOpenApiCriterionByName(openApiCriteria))
        .isNotNull()
        .extracting(OpenApiCriterion::getName)
        .isEqualTo(openApiCriteria.name());
    }

    @Test
    void shouldCreateNewEntity_whenOpenApiCriterionDoesNotExist() {
      var openApiCriteria = PATH_COVERAGE;

      doReturn(Optional.empty())
        .when(openApiCriterionRepositoryMock)
        .findByName(openApiCriteria.name());

      assertThat(fixture.getOpenApiCriterionByName(openApiCriteria))
        .isNotNull()
        .satisfies(
          c -> assertThat(c.getId()).isNull(),
          c -> assertThat(c.getName()).isEqualTo(openApiCriteria.name())
        );
    }
  }
}

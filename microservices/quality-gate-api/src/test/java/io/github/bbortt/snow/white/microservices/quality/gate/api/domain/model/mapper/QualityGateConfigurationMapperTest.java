/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class QualityGateConfigurationMapperTest {

  @Mock
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepositoryMock;

  private QualityGateConfigurationMapper fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateConfigurationMapperImpl();
    fixture.setOpenApiCoverageConfigurationRepository(
      openApiCoverageConfigurationRepositoryMock
    );
  }

  @Nested
  class MapOpenApiCriteriaToStringList {

    @Test
    void shouldReturnStringList() {
      var qualityGateConfiguration = new QualityGateConfiguration()
        .withOpenApiCoverageConfiguration(
          OpenApiCoverageConfiguration.builder().name("foo").build()
        )
        .withOpenApiCoverageConfiguration(
          OpenApiCoverageConfiguration.builder().name("bar").build()
        );

      List<String> stringList = fixture.mapOpenApiCriteriaToStringList(
        qualityGateConfiguration
      );

      assertThat(stringList).containsExactlyInAnyOrder("foo", "bar");
    }

    @Test
    void shouldFilterNullValues() {
      var qualityGateConfiguration = QualityGateConfiguration.builder()
        .openApiCoverageConfigurations(
          Set.of(QualityGateOpenApiCoverageMapping.builder().build())
        )
        .build();

      List<String> stringList = fixture.mapOpenApiCriteriaToStringList(
        qualityGateConfiguration
      );

      assertThat(stringList).isNotNull().isEmpty();
    }

    public static Stream<
      Set<QualityGateOpenApiCoverageMapping>
    > shouldReturnEmptyList_whenApiCoverageConfigurationsIsEmpty() {
      return Stream.of(null, emptySet());
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnEmptyList_whenApiCoverageConfigurationsIsEmpty(
      Set<QualityGateOpenApiCoverageMapping> openApiCoverageConfigurations
    ) {
      var qualityGateConfiguration = QualityGateConfiguration.builder()
        .openApiCoverageConfigurations(openApiCoverageConfigurations)
        .build();

      List<String> stringList = fixture.mapOpenApiCriteriaToStringList(
        qualityGateConfiguration
      );

      assertThat(stringList).isNotNull().isEmpty();
    }
  }

  @Nested
  class MapOpenApiCriteriaToMappings {

    @Test
    void shouldCreateMappingsFromStrings() {
      var openApiCriteria = List.of("foo", "bar");
      var qualityGateConfigurationMock = mock(QualityGateConfiguration.class);

      var openApiCoverageConfigurationMock = mock(
        OpenApiCoverageConfiguration.class
      );
      doReturn(Optional.of(openApiCoverageConfigurationMock))
        .when(openApiCoverageConfigurationRepositoryMock)
        .findByName(anyString());

      Set<
        QualityGateOpenApiCoverageMapping
      > qualityGateOpenApiCoverageMappings =
        fixture.mapOpenApiCriteriaToMappings(
          openApiCriteria,
          qualityGateConfigurationMock
        );

      assertThat(qualityGateOpenApiCoverageMappings)
        .hasSize(2)
        .allSatisfy(mapping ->
          assertThat(mapping.getQualityGateConfiguration()).isEqualTo(
            qualityGateConfigurationMock
          )
        )
        .allSatisfy(mapping ->
          assertThat(mapping.getOpenApiCoverageConfiguration()).isEqualTo(
            openApiCoverageConfigurationMock
          )
        );

      ArgumentCaptor<String> nameCaptor = captor();
      verify(openApiCoverageConfigurationRepositoryMock, times(2)).findByName(
        nameCaptor.capture()
      );
      assertThat(nameCaptor.getAllValues())
        .hasSize(2)
        .containsExactly("foo", "bar");
    }

    @Test
    void shouldThrow_ifOpenApiCriteriaDoesNotExist() {
      var openApiCriteria = singletonList("foo");
      var qualityGateConfigurationMock = mock(QualityGateConfiguration.class);

      doReturn(Optional.empty())
        .when(openApiCoverageConfigurationRepositoryMock)
        .findByName("foo");

      assertThatThrownBy(() ->
        fixture.mapOpenApiCriteriaToMappings(
          openApiCriteria,
          qualityGateConfigurationMock
        )
      ).isInstanceOf(OpenApiCriterionDoesNotExistException.class);

      verifyNoInteractions(qualityGateConfigurationMock);
    }

    public static Stream<
      List<String>
    > shouldReturnEmptyList_whenOpenApiCriteriaIsEmpty() {
      return Stream.of(null, emptyList());
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnEmptyList_whenOpenApiCriteriaIsEmpty(
      List<String> openApiCriteria
    ) {
      var qualityGateConfigurationMock = mock(QualityGateConfiguration.class);

      Set<
        QualityGateOpenApiCoverageMapping
      > qualityGateOpenApiCoverageMappings =
        fixture.mapOpenApiCriteriaToMappings(
          openApiCriteria,
          qualityGateConfigurationMock
        );

      assertThat(qualityGateOpenApiCoverageMappings).isNotNull().isEmpty();

      verifyNoInteractions(qualityGateConfigurationMock);
    }
  }
}

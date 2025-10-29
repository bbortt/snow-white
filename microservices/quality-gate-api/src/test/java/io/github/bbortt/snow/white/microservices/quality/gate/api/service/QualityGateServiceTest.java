/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.SET;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.UnmodifiableConfigurationException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.assertj.core.api.ListAssert;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith({ MockitoExtension.class })
class QualityGateServiceTest {

  @Mock
  private QualityGateConfigurationMapper qualityGateConfigurationMapperMock;

  @Mock
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepositoryMock;

  @Mock
  private QualityGateConfigurationRepository qualityGateConfigurationRepositoryMock;

  private QualityGateService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateService(
      qualityGateConfigurationMapperMock,
      openApiCoverageConfigurationRepositoryMock,
      qualityGateConfigurationRepositoryMock
    );
  }

  @Nested
  class Persist {

    @Test
    void shouldSaveNewConfiguration()
      throws ConfigurationNameAlreadyExistsException {
      var qualityGateConfigurationArgumentCaptor =
        assertThatNewQualityGateConfigurationHasBeenPersisted(emptyList());

      verifyNoInteractions(qualityGateConfigurationMapperMock);

      assertThat(qualityGateConfigurationArgumentCaptor.getValue())
        .isNotNull()
        .extracting(QualityGateConfiguration::getIsPredefined)
        .isEqualTo(FALSE);
    }

    @Test
    void shouldSaveNewConfiguration_withOpenApiCriteriaAttached()
      throws ConfigurationNameAlreadyExistsException {
      var openApiCriteria = singletonList(PATH_COVERAGE.name());

      Set<
        QualityGateOpenApiCoverageMapping
      > qualityGateOpenApiCoverageMappings = mock();

      ArgumentCaptor<QualityGateConfiguration> updatedQualityGateConfiguration =
        captor();
      doReturn(qualityGateOpenApiCoverageMappings)
        .when(qualityGateConfigurationMapperMock)
        .mapOpenApiCriteriaToMappings(
          eq(openApiCriteria),
          updatedQualityGateConfiguration.capture()
        );

      var initialQualityGateConfigurationArgumentCaptor =
        assertThatNewQualityGateConfigurationHasBeenPersisted(openApiCriteria);

      assertThat(initialQualityGateConfigurationArgumentCaptor.getValue())
        .isNotNull()
        .extracting(QualityGateConfiguration::getOpenApiCoverageConfigurations)
        .asInstanceOf(SET)
        .isEmpty();

      assertThat(updatedQualityGateConfiguration.getValue())
        .isNotNull()
        .extracting(QualityGateConfiguration::getOpenApiCoverageConfigurations)
        .isEqualTo(qualityGateOpenApiCoverageMappings);
    }

    ArgumentCaptor<
      QualityGateConfiguration
    > assertThatNewQualityGateConfigurationHasBeenPersisted(
      List<String> openApiCriteria
    ) throws ConfigurationNameAlreadyExistsException {
      var configuration = QualityGateConfiguration.builder()
        .name("NonExistingConfig")
        .build();

      doReturn(false)
        .when(qualityGateConfigurationRepositoryMock)
        .existsByName(configuration.getName());

      var persistedQualityGateConfiguration = new QualityGateConfiguration();
      ArgumentCaptor<
        QualityGateConfiguration
      > qualityGateConfigurationArgumentCaptor = captor();
      doReturn(persistedQualityGateConfiguration)
        .when(qualityGateConfigurationRepositoryMock)
        .save(qualityGateConfigurationArgumentCaptor.capture());

      var result = fixture.persist(configuration, openApiCriteria);

      assertThat(result).isEqualTo(persistedQualityGateConfiguration);

      verify(qualityGateConfigurationRepositoryMock).existsByName(
        configuration.getName()
      );

      return qualityGateConfigurationArgumentCaptor;
    }

    @Test
    void shouldThrow_whenConfigurationAlreadyExists() {
      var configuration = QualityGateConfiguration.builder()
        .name("ExistingConfig")
        .build();

      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsByName(configuration.getName());

      assertThatThrownBy(() ->
        fixture.persist(configuration, emptyList())
      ).isInstanceOf(ConfigurationNameAlreadyExistsException.class);

      verifyNoInteractions(qualityGateConfigurationMapperMock);
      verify(qualityGateConfigurationRepositoryMock, never()).save(
        configuration
      );
    }
  }

  @Nested
  class DeleteByName {

    @Test
    void shouldRemoveExistingConfiguration()
      throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
      var name = "ExistingConfig";
      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsByName(name);

      fixture.deleteByName(name);

      verify(qualityGateConfigurationRepositoryMock).deleteByName(name);
    }

    @Test
    void shouldThrow_whenConfigurationDoesNotExist() {
      var name = "NonExistingConfig";
      doReturn(false)
        .when(qualityGateConfigurationRepositoryMock)
        .existsByName(name);

      assertThatThrownBy(() -> fixture.deleteByName(name)).isInstanceOf(
        ConfigurationDoesNotExistException.class
      );
    }

    @Test
    void shouldThrow_whenConfigurationIsPredefined() {
      var name = "PredefinedConfig";
      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsByName(name);
      doReturn(
        Optional.of(
          QualityGateConfiguration.builder()
            .name(name)
            .isPredefined(TRUE)
            .build()
        )
      )
        .when(qualityGateConfigurationRepositoryMock)
        .findByName(name);

      assertThatThrownBy(() -> fixture.deleteByName(name)).isInstanceOf(
        UnmodifiableConfigurationException.class
      );
    }
  }

  @Nested
  class FindAllQualityGateConfigurations {

    @Mock
    private Pageable pageable;

    @Test
    void shouldReturnAllConfigurationNames() {
      var qualityGateConfigurations = Page.empty();
      doReturn(qualityGateConfigurations)
        .when(qualityGateConfigurationRepositoryMock)
        .findAll(pageable);

      Page<@NonNull QualityGateConfiguration> result =
        fixture.findAllQualityGateConfigurations(pageable);

      assertThat(result).isEqualTo(qualityGateConfigurations);
    }
  }

  @Nested
  class FindByName {

    @Test
    void shouldReturnExistingConfiguration()
      throws ConfigurationDoesNotExistException {
      var name = "ExistingConfig";
      var configuration = QualityGateConfiguration.builder().name(name).build();

      doReturn(Optional.of(configuration))
        .when(qualityGateConfigurationRepositoryMock)
        .findByName(name);

      QualityGateConfiguration result = fixture.findByName(name);

      assertThat(result).isEqualTo(configuration);
    }

    @Test
    void shouldThrow_whenConfigurationDoesNotExists() {
      var name = "NonExistingConfig";

      doReturn(Optional.empty())
        .when(qualityGateConfigurationRepositoryMock)
        .findByName(name);

      assertThatThrownBy(() -> fixture.findByName(name)).isInstanceOf(
        ConfigurationDoesNotExistException.class
      );
    }
  }

  @Nested
  class Update {

    @Test
    void shouldUpdateExistingConfiguration()
      throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
      var name = "ExistingConfig";
      var configuration = QualityGateConfiguration.builder().name(name).build();

      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsByName(name);

      var updatedConfig = QualityGateConfiguration.builder().build();
      doReturn(updatedConfig)
        .when(qualityGateConfigurationRepositoryMock)
        .save(configuration);

      var result = fixture.update(configuration);

      assertThat(result).isEqualTo(updatedConfig);
    }

    @Test
    void shouldThrow_whenConfigurationDoesNotExists() {
      var name = "NonExistingConfig";
      doReturn(false)
        .when(qualityGateConfigurationRepositoryMock)
        .existsByName(name);

      var configuration = QualityGateConfiguration.builder().name(name).build();

      assertThatThrownBy(() -> fixture.update(configuration)).isInstanceOf(
        ConfigurationDoesNotExistException.class
      );
    }

    @Test
    void shouldThrow_whenConfigurationIsPredefined() {
      var name = "PredefinedConfig";
      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsByName(name);
      doReturn(
        Optional.of(
          QualityGateConfiguration.builder()
            .name(name)
            .isPredefined(TRUE)
            .build()
        )
      )
        .when(qualityGateConfigurationRepositoryMock)
        .findByName(name);

      var configuration = QualityGateConfiguration.builder().name(name).build();

      assertThatThrownBy(() -> fixture.update(configuration)).isInstanceOf(
        UnmodifiableConfigurationException.class
      );
    }
  }

  @Nested
  class InitPredefinedQualityGates {

    @Test
    void shouldUpdatePersistedQualityGateConfigurations() {
      doReturnOpenApiCoverageConfigurationByNameUponMockInvocation();

      doAnswer(invocationOnMock ->
        Optional.of(
          QualityGateConfiguration.builder()
            .id(ThreadLocalRandom.current().nextLong())
            .name(invocationOnMock.getArgument(0))
            .build()
        )
      )
        .when(qualityGateConfigurationRepositoryMock)
        .findByName(anyString());

      fixture.initPredefinedQualityGates();

      ArgumentCaptor<
        List<QualityGateConfiguration>
      > defaultQualityGateConfigurationsCaptor = captor();
      verify(qualityGateConfigurationRepositoryMock).saveAll(
        defaultQualityGateConfigurationsCaptor.capture()
      );

      assertThatAllStandardQualityGateConfigurationsWerePersisted(
        assertThat(defaultQualityGateConfigurationsCaptor.getValue())
          .isNotEmpty()
          .allSatisfy(qualityGateConfiguration ->
            assertThat(qualityGateConfiguration.getId()).isNotNull()
          )
      );

      verify(qualityGateConfigurationRepositoryMock, times(4)).findByName(
        anyString()
      );

      verify(openApiCoverageConfigurationRepositoryMock, times(16)).findByName(
        anyString()
      );
    }

    @Test
    void shouldPersistQualityGateConfigurationsIfNoneExistYet() {
      doReturnOpenApiCoverageConfigurationByNameUponMockInvocation();

      doReturn(Optional.empty())
        .when(qualityGateConfigurationRepositoryMock)
        .findByName(anyString());

      fixture.initPredefinedQualityGates();

      ArgumentCaptor<
        List<QualityGateConfiguration>
      > defaultQualityGateConfigurationsCaptor = captor();
      verify(qualityGateConfigurationRepositoryMock).saveAll(
        defaultQualityGateConfigurationsCaptor.capture()
      );

      assertThatAllStandardQualityGateConfigurationsWerePersisted(
        assertThat(
          defaultQualityGateConfigurationsCaptor.getValue()
        ).isNotEmpty()
      );

      verify(qualityGateConfigurationRepositoryMock, times(4)).findByName(
        anyString()
      );

      verify(openApiCoverageConfigurationRepositoryMock, times(16)).findByName(
        anyString()
      );
    }

    private void doReturnOpenApiCoverageConfigurationByNameUponMockInvocation() {
      doAnswer(invocationOnMock ->
        Optional.of(
          OpenApiCoverageConfiguration.builder()
            .name(invocationOnMock.getArgument(0))
            .build()
        )
      )
        .when(openApiCoverageConfigurationRepositoryMock)
        .findByName(anyString());
    }

    private void assertThatAllStandardQualityGateConfigurationsWerePersisted(
      ListAssert<QualityGateConfiguration> qualityGateConfigurationListAssert
    ) {
      qualityGateConfigurationListAssert
        .hasSize(4)
        .satisfiesExactly(
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              c -> assertThat(c.getName()).isEqualTo("basic-coverage"),
              c -> assertThat(c.getOpenApiCoverageConfigurations()).hasSize(5)
            ),
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              c -> assertThat(c.getName()).isEqualTo("full-feature"),
              c -> assertThat(c.getOpenApiCoverageConfigurations()).hasSize(10)
            ),
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              c -> assertThat(c.getName()).isEqualTo("minimal"),
              c -> assertThat(c.getOpenApiCoverageConfigurations()).hasSize(1)
            ),
          qualityGateConfiguration ->
            assertThat(qualityGateConfiguration).satisfies(
              c -> assertThat(c.getName()).isEqualTo("dry-run"),
              c -> assertThat(c.getOpenApiCoverageConfigurations()).isEmpty()
            )
        );
    }
  }
}

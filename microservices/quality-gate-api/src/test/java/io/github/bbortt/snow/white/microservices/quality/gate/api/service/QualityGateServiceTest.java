package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class QualityGateServiceTest {

  @Mock
  private QualityGateConfigurationRepository qualityGateConfigurationRepositoryMock;

  private QualityGateService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateService(qualityGateConfigurationRepositoryMock);
  }

  @Nested
  class Persist {

    @Test
    void shouldSaveNewConfiguration() {
      var configuration = QualityGateConfiguration.builder()
        .name("NonExistingConfig")
        .build();

      doReturn(false)
        .when(qualityGateConfigurationRepositoryMock)
        .existsById(configuration.getName());

      assertThatCode(() -> fixture.persist(configuration)
      ).doesNotThrowAnyException();

      verify(qualityGateConfigurationRepositoryMock).existsById(
        configuration.getName()
      );
      verify(qualityGateConfigurationRepositoryMock).save(configuration);
    }

    @Test
    void shouldThrowException_whenConfigurationAlreadyExists() {
      var configuration = QualityGateConfiguration.builder()
        .name("ExistingConfig")
        .build();

      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsById(configuration.getName());

      assertThatThrownBy(() -> fixture.persist(configuration)).isInstanceOf(
        ConfigurationNameAlreadyExistsException.class
      );

      verify(qualityGateConfigurationRepositoryMock, never()).save(
        configuration
      );
    }
  }

  @Nested
  class DeleteByName {

    @Test
    void shouldRemoveExistingConfiguration()
      throws ConfigurationDoesNotExistException {
      var name = "ExistingConfig";
      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsById(name);

      fixture.deleteByName(name);

      verify(qualityGateConfigurationRepositoryMock).deleteById(name);
    }

    @Test
    void shouldThrowException_whenConfigurationDoesNotExist()
      throws ConfigurationDoesNotExistException {
      var name = "NonExistingConfig";
      doReturn(false)
        .when(qualityGateConfigurationRepositoryMock)
        .existsById(name);

      assertThatThrownBy(() -> fixture.deleteByName(name)).isInstanceOf(
        ConfigurationDoesNotExistException.class
      );
    }
  }

  @Nested
  class GetAllQualityGateConfigNames {

    @Test
    void shouldReturnAllConfigurationNames() {
      var name = "TestConfig";

      doReturn(new HashSet<>(singletonList(name)))
        .when(qualityGateConfigurationRepositoryMock)
        .findAllNames();

      Set<String> result = fixture.getAllQualityGateConfigNames();

      assertThat(result).containsExactly(name);
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
        .findById(name);

      QualityGateConfiguration result = fixture.findByName(name);

      assertThat(result).isEqualTo(configuration);
    }

    @Test
    void shouldThrowException_whenConfigurationDoesNotExists() {
      var name = "NonExistingConfig";

      doReturn(Optional.empty())
        .when(qualityGateConfigurationRepositoryMock)
        .findById(name);

      assertThatThrownBy(() -> fixture.findByName(name)).isInstanceOf(
        ConfigurationDoesNotExistException.class
      );
    }
  }

  @Nested
  class Update {

    @Test
    void shouldUpdateExistingConfiguration()
      throws ConfigurationDoesNotExistException {
      var name = "ExistingConfig";
      var configuration = QualityGateConfiguration.builder().name(name).build();

      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsById(name);

      var updatedConfig = QualityGateConfiguration.builder().build();
      doReturn(updatedConfig)
        .when(qualityGateConfigurationRepositoryMock)
        .save(configuration);

      var result = fixture.update(configuration);

      assertThat(result).isEqualTo(updatedConfig);
    }

    @Test
    void shouldThrowException_whenConfigurationDoesNotExists() {
      var name = "NonExistingConfig";
      doReturn(false)
        .when(qualityGateConfigurationRepositoryMock)
        .existsById(name);

      var configuration = QualityGateConfiguration.builder().name(name).build();

      assertThatThrownBy(() -> fixture.update(configuration)).isInstanceOf(
        ConfigurationDoesNotExistException.class
      );
    }
  }
}

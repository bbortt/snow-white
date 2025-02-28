package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.repository.QualityGateConfigurationRepository;
import java.util.Optional;
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
  void setUp() {
    fixture = new QualityGateService(qualityGateConfigurationRepositoryMock);
  }

  @Nested
  class Persist {

    @Test
    void shouldSaveConfiguration() {
      var configuration = QualityGateConfiguration.builder()
        .name("TestConfig")
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
    void shouldThrowException_whenConfigurationNameAlreadyExists() {
      var configuration = QualityGateConfiguration.builder()
        .name("ExistingConfig")
        .build();

      doReturn(true)
        .when(qualityGateConfigurationRepositoryMock)
        .existsById(configuration.getName());

      assertThatThrownBy(() -> fixture.persist(configuration)).isInstanceOf(
        QualityGateService.ConfigurationNameAlreadyExistsException.class
      );

      verify(qualityGateConfigurationRepositoryMock, never()).save(
        configuration
      );
    }
  }

  @Nested
  class FindByName {

    @Test
    void shouldReturnExistingConfiguration()
      throws QualityGateService.ConfigurationDoesNotExistException {
      var name = "ExistingConfig";
      var configuration = QualityGateConfiguration.builder().name(name).build();

      doReturn(Optional.of(configuration))
        .when(qualityGateConfigurationRepositoryMock)
        .findById(name);

      var result = fixture.findByName(name);

      assertThat(result).isEqualTo(configuration);
    }

    @Test
    void shouldThrowException_whenConfigurationNameDoesNotExists() {
      var name = "NonExistingConfig";

      doReturn(Optional.empty())
        .when(qualityGateConfigurationRepositoryMock)
        .findById(name);

      assertThatThrownBy(() -> fixture.findByName(name)).isInstanceOf(
        QualityGateService.ConfigurationDoesNotExistException.class
      );
    }
  }
}

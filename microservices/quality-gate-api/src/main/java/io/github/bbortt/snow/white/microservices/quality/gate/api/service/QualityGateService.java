package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.repository.QualityGateConfigurationRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QualityGateService {

  private final QualityGateConfigurationRepository qualityGateConfigurationRepository;

  public void persist(
    @Nonnull QualityGateConfiguration qualityGateConfiguration
  ) throws ConfigurationNameAlreadyExistsException {
    if (
      qualityGateConfigurationRepository.existsById(
        qualityGateConfiguration.getName()
      )
    ) {
      throw new ConfigurationNameAlreadyExistsException();
    }

    qualityGateConfigurationRepository.save(qualityGateConfiguration);
  }

  public QualityGateConfiguration findByName(@Nullable String name)
    throws ConfigurationDoesNotExistException {
    return qualityGateConfigurationRepository
      .findById(name)
      .orElseThrow(ConfigurationDoesNotExistException::new);
  }

  public static class ConfigurationNameAlreadyExistsException
    extends Throwable {}

  public static class ConfigurationDoesNotExistException extends Throwable {}
}

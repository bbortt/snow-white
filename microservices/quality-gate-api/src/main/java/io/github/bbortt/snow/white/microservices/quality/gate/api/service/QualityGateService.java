package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QualityGateService {

  private final QualityGateConfigurationRepository qualityGateConfigurationRepository;

  public QualityGateConfiguration persist(
    @Nonnull QualityGateConfiguration qualityGateConfiguration
  ) throws ConfigurationNameAlreadyExistsException {
    if (
      qualityGateConfigurationRepository.existsById(
        qualityGateConfiguration.getName()
      )
    ) {
      throw new ConfigurationNameAlreadyExistsException(
        qualityGateConfiguration.getName()
      );
    }

    return qualityGateConfigurationRepository.save(qualityGateConfiguration);
  }

  public QualityGateConfiguration findByName(@Nullable String name)
    throws ConfigurationDoesNotExistException {
    return qualityGateConfigurationRepository
      .findById(name)
      .orElseThrow(() -> new ConfigurationDoesNotExistException(name));
  }

  public void deleteByName(String name)
    throws ConfigurationDoesNotExistException {
    if (!qualityGateConfigurationRepository.existsById(name)) {
      throw new ConfigurationDoesNotExistException(name);
    }

    qualityGateConfigurationRepository.deleteById(name);
  }

  public Set<String> getAllQualityGateConfigNames() {
    return qualityGateConfigurationRepository.findAllNames();
  }

  public QualityGateConfiguration update(
    QualityGateConfiguration qualityGateConfiguration
  ) throws ConfigurationDoesNotExistException {
    if (
      !qualityGateConfigurationRepository.existsById(
        qualityGateConfiguration.getName()
      )
    ) {
      throw new ConfigurationDoesNotExistException(
        qualityGateConfiguration.getName()
      );
    }

    return qualityGateConfigurationRepository.save(qualityGateConfiguration);
  }
}

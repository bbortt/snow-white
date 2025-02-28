package io.github.bbortt.snow.white.microservices.quality.gate.api.repository;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityGateConfigurationRepository
  extends CrudRepository<QualityGateConfiguration, String> {}

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository;

import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QualityGateConfigurationRepository
  extends CrudRepository<QualityGateConfiguration, String> {}

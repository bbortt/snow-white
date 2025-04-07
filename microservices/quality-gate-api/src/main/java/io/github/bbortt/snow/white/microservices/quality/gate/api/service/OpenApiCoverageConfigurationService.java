package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static java.util.Arrays.stream;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCoverageConfigurationService {

  private final OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  public Set<
    OpenApiCoverageConfiguration
  > getAllOpenapiCoverageConfigurations() {
    return new HashSet<>(openApiCoverageConfigurationRepository.findAll());
  }

  @Transactional
  public void initOpenApiCriteria() {
    logger.info("Updating OpenAPI criteria table");

    stream(OpenApiCriteria.values())
      .map(openApiCriteria ->
        OpenApiCoverageConfiguration.builder()
          .name(openApiCriteria.name())
          .build()
      )
      .forEach(openApiCoverageConfigurationRepository::save);
  }
}

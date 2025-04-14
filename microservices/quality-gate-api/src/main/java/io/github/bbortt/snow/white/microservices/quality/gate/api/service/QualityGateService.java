/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ALL_ERROR_CODES_DOCUMENTED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ALL_NON_ERROR_CODES_DOCUMENTED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ALL_RESPONSE_CODES_DOCUMENTED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_ERROR_FIELDS;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.RESPONSE_CODE_COVERAGE;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
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

  public QualityGateConfiguration findByName(@Nullable String name)
    throws ConfigurationDoesNotExistException {
    return qualityGateConfigurationRepository
      .findById(name)
      .orElseThrow(() -> new ConfigurationDoesNotExistException(name));
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

  public void initPredefinedQualityGates() {
    qualityGateConfigurationRepository.saveAll(
      List.of(getBasicCoverage(), getFullFeature(), getMinimal(), getDryRun())
    );
  }

  private static QualityGateConfiguration getBasicCoverage() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("basic-coverage")
      .description(
        "A pragmatic balance of common expectations without requiring deep error validation."
      )
      .build();

    addAllOpenApiCriteria(
      qualityGateConfiguration,
      Stream.of(
        PATH_COVERAGE,
        HTTP_METHOD_COVERAGE,
        RESPONSE_CODE_COVERAGE,
        REQUIRED_PARAMETER_COVERAGE,
        ALL_RESPONSE_CODES_DOCUMENTED
      )
    );

    return qualityGateConfiguration;
  }

  private static QualityGateConfiguration getFullFeature() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("full-feature")
      .description(
        "The most complete and strict configuration, useful for production-readiness or auditing."
      )
      .build();

    addAllOpenApiCriteria(
      qualityGateConfiguration,
      Stream.of(
        PATH_COVERAGE,
        HTTP_METHOD_COVERAGE,
        RESPONSE_CODE_COVERAGE,
        ERROR_RESPONSE_CODE_COVERAGE,
        REQUIRED_PARAMETER_COVERAGE,
        PARAMETER_COVERAGE,
        REQUIRED_ERROR_FIELDS,
        ALL_RESPONSE_CODES_DOCUMENTED,
        ALL_ERROR_CODES_DOCUMENTED,
        ALL_NON_ERROR_CODES_DOCUMENTED
      )
    );

    return qualityGateConfiguration;
  }

  private static QualityGateConfiguration getMinimal() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("minimal")
      .description(
        "Just enough to ensure the API is reachable at all expected endpoints."
      )
      .build();

    addAllOpenApiCriteria(qualityGateConfiguration, Stream.of(PATH_COVERAGE));

    return qualityGateConfiguration;
  }

  private static QualityGateConfiguration getDryRun() {
    return QualityGateConfiguration.builder()
      .name("dry-run")
      .description(
        "Doesn’t enforce any rules, but may be used to generate reports or test tooling."
      )
      .build();
  }

  private static void addAllOpenApiCriteria(
    QualityGateConfiguration qualityGateConfiguration,
    Stream<OpenApiCriteria> openApiCriteria
  ) {
    openApiCriteria
      .map(openApiCriterion ->
        OpenApiCoverageConfiguration.builder()
          .name(openApiCriterion.name())
          .build()
      )
      .map(openApiCoverageConfiguration ->
        QualityGateOpenApiCoverageMapping.builder()
          .openApiCoverageConfiguration(openApiCoverageConfiguration)
          .qualityGateConfiguration(qualityGateConfiguration)
          .build()
      )
      .forEach(mapping ->
        qualityGateConfiguration.getOpenApiCoverageConfigurations().add(mapping)
      );
  }
}

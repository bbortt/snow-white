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
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.UnmodifiableConfigurationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityGateService {

  private final OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;
  private final QualityGateConfigurationRepository qualityGateConfigurationRepository;

  public QualityGateConfiguration persist(
    @Nonnull QualityGateConfiguration qualityGateConfiguration
  ) throws ConfigurationNameAlreadyExistsException {
    if (
      qualityGateConfigurationRepository.existsByName(
        qualityGateConfiguration.getName()
      )
    ) {
      throw new ConfigurationNameAlreadyExistsException(
        qualityGateConfiguration.getName()
      );
    }

    return qualityGateConfigurationRepository.save(
      qualityGateConfiguration.withIsPredefined(FALSE)
    );
  }

  public void deleteByName(String name)
    throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
    if (!qualityGateConfigurationRepository.existsByName(name)) {
      throw new ConfigurationDoesNotExistException(name);
    } else if (
      qualityGateConfigurationRepository
        .findByName(name)
        .map(QualityGateConfiguration::getIsPredefined)
        .orElse(FALSE)
    ) {
      throw new UnmodifiableConfigurationException(name);
    }

    qualityGateConfigurationRepository.deleteByName(name);
  }

  public Page<QualityGateConfiguration> findAllQualityGateConfigurations(
    Pageable pageable
  ) {
    return qualityGateConfigurationRepository.findAll(pageable);
  }

  public QualityGateConfiguration findByName(@Nullable String name)
    throws ConfigurationDoesNotExistException {
    return qualityGateConfigurationRepository
      .findByName(name)
      .orElseThrow(() -> new ConfigurationDoesNotExistException(name));
  }

  public QualityGateConfiguration update(
    QualityGateConfiguration qualityGateConfiguration
  )
    throws ConfigurationDoesNotExistException, UnmodifiableConfigurationException {
    var name = qualityGateConfiguration.getName();
    if (!qualityGateConfigurationRepository.existsByName(name)) {
      throw new ConfigurationDoesNotExistException(name);
    } else if (
      qualityGateConfigurationRepository
        .findByName(name)
        .map(QualityGateConfiguration::getIsPredefined)
        .orElse(FALSE)
    ) {
      throw new UnmodifiableConfigurationException(name);
    }

    return qualityGateConfigurationRepository.save(qualityGateConfiguration);
  }

  @Transactional
  public void initPredefinedQualityGates() {
    logger.info(
      "Updating Quality-Gate configuration table with default values"
    );

    var qualityGateConfigurations = Stream.of(
      getBasicCoverage(),
      getFullFeature(),
      getMinimal(),
      getDryRun()
    )
      .map(qualityGateConfiguration ->
        qualityGateConfigurationRepository
          .findByName(qualityGateConfiguration.getName())
          .map(existingQualityGateConfiguration ->
            qualityGateConfiguration.withId(
              existingQualityGateConfiguration.getId()
            )
          )
          .orElse(qualityGateConfiguration)
      )
      .toList();

    qualityGateConfigurationRepository.saveAll(qualityGateConfigurations);
  }

  private QualityGateConfiguration getBasicCoverage() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("basic-coverage")
      .description(
        "A pragmatic balance of common expectations without requiring deep error validation."
      )
      .isPredefined(TRUE)
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

  private QualityGateConfiguration getFullFeature() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("full-feature")
      .description(
        "The most complete and strict configuration, useful for production-readiness or auditing."
      )
      .isPredefined(TRUE)
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

  private QualityGateConfiguration getMinimal() {
    var qualityGateConfiguration = QualityGateConfiguration.builder()
      .name("minimal")
      .description(
        "Just enough to ensure the API is reachable at all expected endpoints."
      )
      .isPredefined(TRUE)
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
      .isPredefined(TRUE)
      .build();
  }

  private void addAllOpenApiCriteria(
    QualityGateConfiguration qualityGateConfiguration,
    Stream<OpenApiCriteria> openApiCriteria
  ) {
    openApiCriteria
      .map(openApiCriterion ->
        openApiCoverageConfigurationRepository
          .findByName(openApiCriterion.name())
          .orElseThrow(IllegalStateException::new)
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

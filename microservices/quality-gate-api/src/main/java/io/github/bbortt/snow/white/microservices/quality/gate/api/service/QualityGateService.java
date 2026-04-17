/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.service;

import static java.lang.Boolean.FALSE;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper.QualityGateConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.QualityGateConfigurationRepository;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationDoesNotExistException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.ConfigurationNameAlreadyExistsException;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.exception.UnmodifiableConfigurationException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityGateService {

  private final QualityGateConfigurationMapper qualityGateConfigurationMapper;

  private final DefaultOpenApiQualityGates defaultOpenApiQualityGates;
  private final QualityGateConfigurationRepository qualityGateConfigurationRepository;

  @Transactional
  public QualityGateConfiguration persist(
    @NonNull QualityGateConfiguration qualityGateConfiguration,
    @Nullable List<String> openApiCriteria
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

    var initialQualityGateConfiguration =
      qualityGateConfigurationRepository.save(
        qualityGateConfiguration.withIsPredefined(FALSE)
      );

    if (!isEmpty(openApiCriteria)) {
      attachOpenApiCoverageConfigurations(
        initialQualityGateConfiguration,
        openApiCriteria
      );
    }

    return initialQualityGateConfiguration;
  }

  private void attachOpenApiCoverageConfigurations(
    QualityGateConfiguration target,
    List<String> openApiCriteria
  ) {
    var openApiCoverageConfigurations =
      qualityGateConfigurationMapper.mapOpenApiCriteriaToMappings(
        openApiCriteria,
        target
      );

    target.setOpenApiCoverageConfigurations(openApiCoverageConfigurations);
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

  public Page<
    @NonNull QualityGateConfiguration
  > findAllQualityGateConfigurations(Pageable pageable) {
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

    var qualityGateConfigurations = defaultOpenApiQualityGates
      .getDefaultOpenApiCoverageConfigurations()
      .stream()
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
}

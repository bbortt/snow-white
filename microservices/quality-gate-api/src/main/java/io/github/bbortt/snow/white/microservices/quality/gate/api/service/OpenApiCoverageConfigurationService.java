/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

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

    var missingOpenApiCriteria = stream(OpenApiCriteria.values())
      .filter(openApiCriterion ->
        !openApiCoverageConfigurationRepository.existsByName(
          openApiCriterion.name()
        )
      )
      .map(openApiCriteria ->
        OpenApiCoverageConfiguration.builder()
          .name(openApiCriteria.name())
          .build()
      )
      .toList();

    if (missingOpenApiCriteria.isEmpty()) {
      logger.debug(
        "All OpenApi criteria are already present in database, nothing to do"
      );
      return;
    }

    logger.debug(
      "The following OpenAPI criteria are missing and will be persisted: {}",
      missingOpenApiCriteria
    );

    openApiCoverageConfigurationRepository.saveAll(missingOpenApiCriteria);
  }
}

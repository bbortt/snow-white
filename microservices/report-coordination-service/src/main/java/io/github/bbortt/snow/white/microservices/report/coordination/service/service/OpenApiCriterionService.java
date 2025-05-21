/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static java.util.Arrays.stream;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.OpenApiCriterionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCriterionService {

  private final OpenApiCriterionRepository openApiCriterionRepository;

  public void initOpenApiTestCriteria() {
    logger.info("Updating OpenAPI criteria table");

    var missingOpenApiTestCriteria = stream(OpenApiCriteria.values())
      .filter(openApiCriterion ->
        !openApiCriterionRepository.existsByName(openApiCriterion.name())
      )
      .map(openApiCriteria ->
        OpenApiTestCriteria.builder().name(openApiCriteria.name()).build()
      )
      .toList();

    if (missingOpenApiTestCriteria.isEmpty()) {
      logger.debug(
        "All OpenApi criteria are already present in database, nothing to do"
      );
      return;
    }

    logger.debug(
      "The following OpenAPI criteria are missing and will be persisted: {}",
      missingOpenApiTestCriteria
    );

    openApiCriterionRepository.saveAll(missingOpenApiTestCriteria);
  }
}

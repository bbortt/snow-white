/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.resource;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.CriteriaApi;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper.OpenApiCoverageConfigurationMapper;
import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CriteriaResource implements CriteriaApi {

  private final OpenApiCoverageConfigurationMapper openApiCoverageConfigurationMapper;
  private final OpenApiCoverageConfigurationService openApiCoverageConfigurationService;

  @Override
  public ResponseEntity<List<OpenApiCriterion>> listOpenApiCriteria() {
    var openApiCoverageConfigurations = openApiCoverageConfigurationService
      .getAllOpenapiCoverageConfigurations()
      .stream()
      .sorted(comparing(OpenApiCoverageConfiguration::getName))
      .collect(toCollection(LinkedHashSet::new));

    return ResponseEntity.ok(
      openApiCoverageConfigurationMapper.toDtos(openApiCoverageConfigurations)
    );
  }
}

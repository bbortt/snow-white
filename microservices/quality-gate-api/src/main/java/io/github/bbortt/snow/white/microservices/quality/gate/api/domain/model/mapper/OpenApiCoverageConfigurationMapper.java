/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface OpenApiCoverageConfigurationMapper {
  default List<OpenApiCriterion> toDtos(
    Set<OpenApiCoverageConfiguration> openApiCoverageConfigurations
  ) {
    return openApiCoverageConfigurations
      .parallelStream()
      .map(openApiCoverageConfiguration -> {
        var openApiCriteria = OpenApiCriteria.valueOf(
          openApiCoverageConfiguration.getName()
        );

        return OpenApiCriterion.builder()
          .id(openApiCriteria.name())
          .name(openApiCriteria.getLabel())
          .description(openApiCriteria.getDescription())
          .build();
      })
      .toList();
  }
}

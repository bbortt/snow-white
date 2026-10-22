/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;

@Mapper(componentModel = SPRING)
public interface OpenApiCoverageConfigurationMapper {
  /**
   * The persisted row only records a criterion's existence by name; its label and description
   * are looked up from the {@link OpenApiCoverageCriteria} enum at read time, never stored.
   */
  @RealizesArch(ArchTraceables.ARCH_002_CRITERIA_METADATA_OWNED_BY_ENUM)
  default List<OpenApiCriterion> toDtos(
    Set<OpenApiCoverageConfiguration> openApiCoverageConfigurations
  ) {
    return openApiCoverageConfigurations
      .parallelStream()
      .map(openApiCoverageConfiguration -> {
        var openApiCriteria = OpenApiCoverageCriteria.valueOf(
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

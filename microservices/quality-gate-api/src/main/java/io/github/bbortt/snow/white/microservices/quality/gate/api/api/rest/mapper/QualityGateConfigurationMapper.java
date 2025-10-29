/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper;

import static lombok.AccessLevel.PACKAGE;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR)
public abstract class QualityGateConfigurationMapper {

  @Autowired
  @Setter(PACKAGE)
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  @Mapping(
    target = "openApiCriteria",
    expression = "java(mapOpenApiCriteriaToStringList(entity))"
  )
  public abstract QualityGateConfig toDto(QualityGateConfiguration entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "openApiCoverageConfigurations", ignore = true)
  public abstract QualityGateConfiguration toInitialEntityIgnoringRelationships(
    QualityGateConfig dto
  );

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", source = "dto.name")
  @Mapping(target = "description", source = "dto.description")
  @Mapping(target = "isPredefined", source = "dto.isPredefined")
  @Mapping(
    target = "openApiCoverageConfigurations",
    expression = "java(mapOpenApiCriteriaToMappings(dto.getOpenApiCriteria(), existingEntity))"
  )
  public abstract QualityGateConfiguration toEntityForUpdate(
    QualityGateConfig dto,
    QualityGateConfiguration existingEntity
  ) throws OpenApiCriterionDoesNotExistException;

  protected List<String> mapOpenApiCriteriaToStringList(
    @NonNull QualityGateConfiguration entity
  ) {
    return entity
      .getOpenApiCoverageConfigurations()
      .stream()
      .map(mapping -> mapping.getOpenApiCoverageConfiguration().getName())
      .toList();
  }

  public Set<QualityGateOpenApiCoverageMapping> mapOpenApiCriteriaToMappings(
    @Nullable List<String> openApiCriteria,
    @NonNull QualityGateConfiguration existingEntity
  ) throws OpenApiCriterionDoesNotExistException {
    if (isEmpty(openApiCriteria)) {
      return new HashSet<>();
    }

    Set<QualityGateOpenApiCoverageMapping> mappings = new HashSet<>();

    for (var criteriaName : new HashSet<>(openApiCriteria)) {
      OpenApiCoverageConfiguration openApiCoverageConfiguration =
        openApiCoverageConfigurationRepository
          .findByName(criteriaName)
          .orElseThrow(() ->
            new OpenApiCriterionDoesNotExistException(criteriaName)
          );

      var mapping = QualityGateOpenApiCoverageMapping.builder()
        .openApiCoverageConfiguration(openApiCoverageConfiguration)
        .qualityGateConfiguration(existingEntity)
        .build();

      mappings.add(mapping);
    }

    return mappings;
  }
}

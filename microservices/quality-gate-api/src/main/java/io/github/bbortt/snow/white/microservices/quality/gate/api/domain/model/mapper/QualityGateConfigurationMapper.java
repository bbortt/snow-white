/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.mapper;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PACKAGE;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.OpenApiCoverageConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model.QualityGateOpenApiCoverageMapping;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.repository.OpenApiCoverageConfigurationRepository;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Null;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR)
public abstract class QualityGateConfigurationMapper {

  @Autowired
  @Setter(PACKAGE)
  private OpenApiCoverageConfigurationRepository openApiCoverageConfigurationRepository;

  @Mapping(
    target = "openapiCriteria",
    expression = "java(mapOpenApiCriteriaToStringList(entity))"
  )
  public abstract QualityGateConfig toDto(QualityGateConfiguration entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(
    target = "openApiCoverageConfigurations",
    expression = "java(mapOpenApiCriteriaToMappings(dto.getOpenapiCriteria(), null))"
  )
  public abstract QualityGateConfiguration toEntity(QualityGateConfig dto)
    throws OpenApiCriterionDoesNotExistException;

  protected List<String> mapOpenApiCriteriaToStringList(
    @Nonnull QualityGateConfiguration entity
  ) {
    if (isNull(entity.getOpenApiCoverageConfigurations())) {
      return List.of();
    }

    return entity
      .getOpenApiCoverageConfigurations()
      .stream()
      .filter(mapping -> nonNull(mapping.getOpenApiCoverageConfiguration()))
      .map(mapping -> mapping.getOpenApiCoverageConfiguration().getName())
      .toList();
  }

  protected Set<QualityGateOpenApiCoverageMapping> mapOpenApiCriteriaToMappings(
    @Null List<String> openapiCriteria,
    @Nullable QualityGateConfiguration existingEntity
  ) throws OpenApiCriterionDoesNotExistException {
    if (isEmpty(openapiCriteria)) {
      return new HashSet<>();
    }

    Set<QualityGateOpenApiCoverageMapping> mappings = new HashSet<>();

    for (var criteriaName : openapiCriteria) {
      OpenApiCoverageConfiguration coverage =
        openApiCoverageConfigurationRepository
          .findByName(criteriaName)
          .orElseThrow(() ->
            new OpenApiCriterionDoesNotExistException(criteriaName)
          );

      var mapping = QualityGateOpenApiCoverageMapping.builder()
        .openApiCoverageConfiguration(coverage)
        .qualityGateConfiguration(existingEntity)
        .build();

      mappings.add(mapping);
    }

    return mappings;
  }
}

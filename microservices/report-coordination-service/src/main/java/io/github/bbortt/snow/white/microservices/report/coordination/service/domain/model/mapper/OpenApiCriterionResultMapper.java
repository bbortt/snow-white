/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static java.lang.Boolean.TRUE;
import static lombok.AccessLevel.PACKAGE;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCriterionResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.OpenApiCriterionRepository;
import java.util.Set;
import lombok.Setter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR)
public abstract class OpenApiCriterionResultMapper {

  private static final String INCLUDED_REPORT_MAPPER =
    "openApiCriteriaToIncludedInReport";
  private static final String OPENAPI_CRITERION_MAPPER =
    "getOpenApiCriterionByName";

  @Autowired
  @Setter(PACKAGE)
  private OpenApiCriterionRepository openApiCriterionRepository;

  public abstract Set<OpenApiCriterionResult> map(
    Set<
      io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult
    > openApiCriterionResults
  );

  @Mapping(target = "id", ignore = true)
  @Mapping(
    target = "openApiCriterion",
    source = "openApiCriteria",
    qualifiedByName = OPENAPI_CRITERION_MAPPER
  )
  @Mapping(
    target = "includedInReport",
    source = "openApiCriteria",
    qualifiedByName = INCLUDED_REPORT_MAPPER
  )
  @Mapping(target = "qualityGateReport", ignore = true)
  public abstract OpenApiCriterionResult map(
    io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult source
  );

  @Named(INCLUDED_REPORT_MAPPER)
  protected Boolean openApiCriteriaToIncludedInReport(
    OpenApiCriteria openApiCriteria
  ) {
    return TRUE;
  }

  @Named(OPENAPI_CRITERION_MAPPER)
  protected OpenApiCriterion getOpenApiCriterionByName(
    OpenApiCriteria openApiCriteria
  ) {
    String criterionName = openApiCriteria.name();
    return openApiCriterionRepository
      .findByName(criterionName)
      .orElseGet(() ->
        OpenApiCriterion.builder().name(openApiCriteria.name()).build()
      );
  }
}

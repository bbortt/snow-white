/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static java.lang.Boolean.TRUE;
import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = SPRING, injectionStrategy = CONSTRUCTOR)
public abstract class OpenApiTestResultMapper {

  private static final String INCLUDED_REPORT_MAPPER =
    "openApiCriteriaToIncludedInReport";

  public abstract Set<OpenApiTestResult> fromDto(
    Set<
      io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult
    > openApiTestResults
  );

  @Mapping(
    target = "openApiTestCriteria",
    expression = "java(source.openApiCriteria().name())"
  )
  @Mapping(
    target = "includedInReport",
    source = "openApiCriteria",
    qualifiedByName = INCLUDED_REPORT_MAPPER
  )
  @Mapping(target = "qualityGateReport", ignore = true)
  public abstract OpenApiTestResult fromDto(
    io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult source
  );

  @Named(INCLUDED_REPORT_MAPPER)
  protected Boolean openApiCriteriaToIncludedInReport(
    OpenApiCriteria openApiCriteria
  ) {
    return TRUE;
  }
}

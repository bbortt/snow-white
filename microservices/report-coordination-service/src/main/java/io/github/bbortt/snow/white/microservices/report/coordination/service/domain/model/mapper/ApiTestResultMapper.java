/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate202ResponseInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTestResult;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ApiTestResultMapper {
  Set<ApiTestResult> fromDtos(Set<OpenApiTestResult> openApiTestResults);

  @Mapping(
    target = "testCriteria",
    expression = "java(openApiTestResults.getOpenApiCriteria().name())"
  )
  @Mapping(target = "includedInReport", ignore = true)
  @Mapping(target = "apiTest", ignore = true)
  ApiTestResult fromDto(OpenApiTestResult openApiTestResults);

  @Mapping(target = "id", source = "testCriteria")
  @Mapping(target = "isIncludedInQualityGate", source = "includedInReport")
  CalculateQualityGate202ResponseInterfacesInnerTestResultsInner toDto(
    ApiTestResult apiTestResult
  );

  @Mapping(target = "id", source = "testCriteria")
  @Mapping(target = "isIncludedInQualityGate", source = "includedInReport")
  ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner toListDto(
    ApiTestResult apiTestResult
  );
}

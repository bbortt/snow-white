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
    target = "apiTestCriteria",
    expression = "java(openApiTestResults.openApiCriteria().name())"
  )
  @Mapping(target = "includedInReport", ignore = true)
  @Mapping(target = "apiTest", ignore = true)
  ApiTestResult fromDto(OpenApiTestResult openApiTestResults);

  @Mapping(target = "id", source = "apiTestCriteria")
  @Mapping(target = "isIncludedInQualityGate", source = "includedInReport")
  Set<
    CalculateQualityGate202ResponseInterfacesInnerTestResultsInner
  > toTestResults(Set<ApiTestResult> apiTestResults);

  @Mapping(target = "id", source = "apiTestCriteria")
  @Mapping(target = "isIncludedInQualityGate", source = "includedInReport")
  CalculateQualityGate202ResponseInterfacesInnerTestResultsInner toTestResult(
    ApiTestResult apiTestResult
  );

  @Mapping(target = "id", source = "apiTestCriteria")
  @Mapping(target = "isIncludedInQualityGate", source = "includedInReport")
  ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner toListTestResult(
    ApiTestResult apiTestResult
  );
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static java.util.stream.Collectors.toSet;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202ResponseInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ApiTestResultMapper {
  default Set<ApiTestResult> fromDtos(
    Set<OpenApiTestResult> openApiTestResults,
    ApiTest apiTest
  ) {
    return openApiTestResults
      .parallelStream()
      .map(openApiTestResult -> fromDto(openApiTestResult, apiTest))
      .collect(toSet());
  }

  @Mapping(
    target = "apiTestCriteria",
    expression = "java(openApiTestResults.openApiCriteria().name())"
  )
  @Mapping(target = "includedInReport", constant = "false")
  @Mapping(target = "apiTest", source = "apiTest")
  ApiTestResult fromDto(OpenApiTestResult openApiTestResults, ApiTest apiTest);

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

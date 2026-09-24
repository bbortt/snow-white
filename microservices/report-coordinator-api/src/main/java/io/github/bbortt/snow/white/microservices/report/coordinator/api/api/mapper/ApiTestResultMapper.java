/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static java.util.stream.Collectors.toSet;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202ResponseInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingEvidence;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus;
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

  /**
   * The findings are attached here rather than by a generated mapping, because each one carries the
   * back-reference to the result it explains and that reference is what writes its foreign key.
   */
  @RealizesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
  default ApiTestResult fromDto(
    OpenApiTestResult openApiTestResults,
    ApiTest apiTest
  ) {
    var apiTestResult = toApiTestResult(openApiTestResults, apiTest);

    openApiTestResults
      .findings()
      .stream()
      .map(finding -> toFinding(finding, apiTestResult))
      .forEach(apiTestResult.getFindings()::add);

    return apiTestResult;
  }

  /**
   * The result without its findings; use {@link #fromDto(OpenApiTestResult, ApiTest)}.
   */
  @Mapping(
    target = "apiTestCriteria",
    expression = "java(openApiTestResults.openApiCriteria().name())"
  )
  @Mapping(target = "includedInReport", constant = "false")
  @Mapping(target = "apiTest", source = "apiTest")
  @Mapping(target = "findings", ignore = true)
  ApiTestResult toApiTestResult(
    OpenApiTestResult openApiTestResults,
    ApiTest apiTest
  );

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "apiTestResult", source = "apiTestResult")
  @Mapping(
    target = "status",
    expression = "java(findingStatus(finding.status()).getVal())"
  )
  ApiTestFinding toFinding(
    io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding finding,
    ApiTestResult apiTestResult
  );

  FindingEvidence toEvidence(
    io.github.bbortt.snow.white.commons.event.dto.FindingEvidence evidence
  );

  /**
   * Translated constant by constant rather than by name, so that a status added to the event
   * contract fails this compilation instead of reaching the database as a surprise.
   */
  default FindingStatus findingStatus(
    io.github.bbortt.snow.white.commons.event.dto.FindingStatus status
  ) {
    return switch (status) {
      case COVERED -> FindingStatus.COVERED;
      case UNCOVERED -> FindingStatus.UNCOVERED;
      case NOT_APPLICABLE -> FindingStatus.NOT_APPLICABLE;
    };
  }

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

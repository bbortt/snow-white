/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.toSet;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202ResponseInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.GetReportByCalculationId200ResponseInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInnerEvidenceInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInnerTestResultsInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingEvidence;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = SPRING)
public interface ApiTestResultMapper {
  /**
   * A total order over the findings of one criterion result.
   * <p>
   * The spec pointer alone does not order them: an inverted criterion draws its targets from the
   * telemetry, so every status code observed on one operation is a separate finding under that
   * operation's pointer. The discriminators separate those, in the order a reader scans them -
   * the operation first, then the dimension judged within it - and the primary key settles anything
   * they leave equal, so the order never depends on which findings a set happened to hash where.
   */
  Comparator<ApiTestFinding> FINDING_ORDER = Comparator.<
    ApiTestFinding,
    String
  >comparing(ApiTestFinding::getSpecPointer)
    .thenComparing(ApiTestFinding::getHttpMethod, nullsLast(naturalOrder()))
    .thenComparing(ApiTestFinding::getHttpPath, nullsLast(naturalOrder()))
    .thenComparing(ApiTestFinding::getResponseCode, nullsLast(naturalOrder()))
    .thenComparing(ApiTestFinding::getParameterName, nullsLast(naturalOrder()))
    .thenComparing(ApiTestFinding::getContentType, nullsLast(naturalOrder()))
    .thenComparing(ApiTestFinding::getId, nullsLast(naturalOrder()));

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

  /**
   * The single-report shape, which is {@link #toListTestResult(ApiTestResult)} plus the findings
   * behind the coverage. Only reachable with the findings fetched - see
   * {@code QualityGateReportRepository#findWithFindingsByCalculationId(java.util.UUID)} - because
   * an empty array here means "this result has none", never "nobody loaded them".
   */
  @RealizesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  @Mapping(target = "id", source = "apiTestCriteria")
  @Mapping(target = "isIncludedInQualityGate", source = "includedInReport")
  GetReportByCalculationId200ResponseInterfacesInnerTestResultsInner toReportTestResult(
    ApiTestResult apiTestResult
  );

  /**
   * Ordered by {@link #FINDING_ORDER}, because the entity declares no {@code equals} and its
   * identity {@code hashCode} would otherwise let a {@code HashSet} hand the same findings to two
   * identical requests in two different orders - a drilldown that reshuffles on refresh, and an
   * assertion that passes until it does not.
   */
  @RealizesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  @RealizesCon(ConTraceables.CON_001_DETERMINISTIC_ANALYSIS_RESULTS)
  default List<GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInner> toReportFindings(
    Set<ApiTestFinding> findings
  ) {
    return findings
      .stream()
      .sorted(FINDING_ORDER)
      .map(this::toReportFinding)
      .toList();
  }

  GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInner toReportFinding(
    ApiTestFinding apiTestFinding
  );

  GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInnerEvidenceInner toReportEvidence(
    FindingEvidence findingEvidence
  );

  /**
   * The name, not the stored code: the response is read by a human looking at a drilldown, and
   * {@code 1} says nothing. Translated constant by constant for the same reason as
   * {@link #findingStatus(io.github.bbortt.snow.white.commons.event.dto.FindingStatus)}.
   */
  @RealizesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  default GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInner.StatusEnum toReportFindingStatus(
    FindingStatus status
  ) {
    return switch (status) {
      case COVERED -> GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInner.StatusEnum.COVERED;
      case UNCOVERED -> GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInner.StatusEnum.UNCOVERED;
      case NOT_APPLICABLE -> GetReportByCalculationId200ResponseInterfacesInnerTestResultsInnerFindingsInner.StatusEnum.NOT_APPLICABLE;
    };
  }
}

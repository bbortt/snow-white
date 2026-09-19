/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.util.Objects.nonNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
final class ApiTestResultLinker {

  private final ApiTestRepository apiTestRepository;

  /**
   * A criterion outside the gate is persisted but flagged not-included; only included results
   * decide the verdict, and the gate's {@code minCoveragePercentage} is the bar both for a single
   * criterion and for the share of criteria clearing it.
   * A redelivered result for a criterion already present on the {@code ApiTest} replaces it rather
   * than accumulating beside it: the incoming results are removed from the set by identity before
   * being re-added, so a redelivery never leaves two entries for the same criterion.
   */
  @RealizesSw(SwTraceables.SW_016_API_TEST_VERDICT_IS_GATE_SCOPED)
  @RealizesSw(
    SwTraceables.SW_020_REDELIVERED_CRITERION_RESULT_REPLACES_EXISTING_ONE
  )
  void addApiTestResultsToApiTest(
    Set<ApiTestResult> apiTestResults,
    ApiTest apiTest,
    Set<String> includedOpenApiCoverageCriteria,
    int minCoveragePercentage
  ) {
    if (isEmpty(apiTestResults)) {
      return;
    }

    var incomingResults = apiTestResults
      .stream()
      .map(apiTestResult ->
        apiTestResult.withIncludedInReport(
          isIncludedInReport(apiTestResult, includedOpenApiCoverageCriteria)
        )
      )
      .toList();

    var existingResults = apiTest.getApiTestResults();
    if (existingResults.removeAll(incomingResults)) {
      // A replaced result shares its composite primary key (apiTestCriteria, apiTest) with the
      // incoming one; without flushing the orphan removal first, Hibernate would order the new
      // row's insert before the old row's delete and collide on that key.
      apiTestRepository.saveAndFlush(apiTest);
    }
    existingResults.addAll(incomingResults);

    apiTestRepository.save(
      apiTest.withReportStatus(
        deriveApiTestStatus(apiTest, minCoveragePercentage)
      )
    );
  }

  private boolean isIncludedInReport(
    ApiTestResult apiTestResult,
    Set<String> includedOpenApiCoverageCriteria
  ) {
    return (
      nonNull(includedOpenApiCoverageCriteria) &&
      includedOpenApiCoverageCriteria.contains(
        apiTestResult.getApiTestCriteria()
      )
    );
  }

  private ReportStatus deriveApiTestStatus(
    ApiTest apiTest,
    int minCoveragePercentage
  ) {
    var threshold = BigDecimal.valueOf(minCoveragePercentage).divide(
      BigDecimal.valueOf(100),
      2,
      RoundingMode.UNNECESSARY
    );

    var includedResults = apiTest
      .getApiTestResults()
      .stream()
      .filter(ApiTestResult::getIncludedInReport)
      .toList();

    if (includedResults.isEmpty()) {
      return PASSED;
    }

    long passedCount = includedResults
      .stream()
      .filter(r -> r.getCoverage().compareTo(threshold) >= 0)
      .count();

    BigDecimal passRate = BigDecimal.valueOf(passedCount)
      .divide(
        BigDecimal.valueOf(includedResults.size()),
        2,
        RoundingMode.HALF_UP
      )
      .multiply(BigDecimal.valueOf(100));

    return passRate.compareTo(BigDecimal.valueOf(minCoveragePercentage)) >= 0
      ? PASSED
      : FAILED;
  }
}

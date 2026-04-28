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

  void addApiTestResultsToApiTest(
    Set<ApiTestResult> apiTestResults,
    ApiTest apiTest,
    Set<String> includedOpenApiCriteria,
    int minCoveragePercentage
  ) {
    if (isEmpty(apiTestResults)) {
      return;
    }

    apiTestResults
      .parallelStream()
      .map(apiTestResult ->
        apiTestResult.withIncludedInReport(
          isIncludedInReport(apiTestResult, includedOpenApiCriteria)
        )
      )
      .forEach(apiTest.getApiTestResults()::add);

    apiTestRepository.save(
      apiTest.withReportStatus(
        deriveApiTestStatus(apiTest, minCoveragePercentage)
      )
    );
  }

  private boolean isIncludedInReport(
    ApiTestResult apiTestResult,
    Set<String> includedOpenApiCriteria
  ) {
    return (
      nonNull(includedOpenApiCriteria) &&
      includedOpenApiCriteria.contains(apiTestResult.getApiTestCriteria())
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

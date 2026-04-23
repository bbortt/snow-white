/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.util.Objects.nonNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
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
    Set<String> includedOpenApiCriteria
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
      apiTest.withReportStatus(deriveApiTestStatus(apiTest))
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

  private ReportStatus deriveApiTestStatus(ApiTest apiTest) {
    boolean anyIncludedFailed = apiTest
      .getApiTestResults()
      .stream()
      .filter(ApiTestResult::getIncludedInReport)
      .anyMatch(r -> !ONE.equals(r.getCoverage()));

    return anyIncludedFailed ? FAILED : PASSED;
  }
}

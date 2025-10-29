/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static java.util.Objects.nonNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class ApiTestResultLinker {

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
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka.listener;

import static java.util.Objects.nonNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTestResult;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class ApiTestResultLinker {

  void addResultsToApiTest(
    ApiTest apiTest,
    Set<String> includedOpenApiCriteria,
    Set<ApiTestResult> apiTestResults
  ) {
    if (isEmpty(apiTestResults)) {
      return;
    }

    apiTestResults
      .parallelStream()
      .map(apiTestResult ->
        apiTestResult
          .withApiTest(apiTest)
          .withIncludedInReport(
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

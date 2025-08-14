/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka.listener;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;

final class QualityGateStatusCalculator {

  QualityGateReport withUpdatedReportStatus(
    QualityGateReport qualityGateReport
  ) {
    if (isEmpty(qualityGateReport.getApiTests())) {
      return qualityGateReport.withReportStatus(NOT_STARTED);
    } else if (
      qualityGateReport
        .getApiTests()
        .parallelStream()
        .anyMatch(apiTest -> isEmpty(apiTest.getApiTestResults()))
    ) {
      return qualityGateReport.withReportStatus(IN_PROGRESS);
    } else if (
      qualityGateReport
        .getApiTests()
        .parallelStream()
        .anyMatch(apiTest ->
          apiTest
            .getApiTestResults()
            .parallelStream()
            .filter(ApiTestResult::getIncludedInReport)
            .anyMatch(apiTestResult -> !ONE.equals(apiTestResult.getCoverage()))
        )
    ) {
      return qualityGateReport.withReportStatus(FAILED);
    } else {
      return qualityGateReport.withReportStatus(PASSED);
    }
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FINISHED_EXCEPTIONALLY;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.TIMED_OUT;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
final class QualityGateStatusCalculator {

  @VisibleForTesting
  static final List<ReportStatus> TERMINAL_STATUS = List.of(
    FAILED,
    FINISHED_EXCEPTIONALLY,
    TIMED_OUT
  );

  QualityGateReport withUpdatedReportStatus(
    QualityGateReport qualityGateReport
  ) {
    if (TERMINAL_STATUS.contains(qualityGateReport.getReportStatus())) {
      return qualityGateReport;
    } else if (isEmpty(qualityGateReport.getApiTests())) {
      return qualityGateReport.withReportStatus(NOT_STARTED);
    } else if (anyApiTestInReportStatus(qualityGateReport, IN_PROGRESS)) {
      return qualityGateReport.withReportStatus(IN_PROGRESS);
    } else if (anyApiTestInReportStatus(qualityGateReport, FAILED)) {
      return qualityGateReport.withReportStatus(FAILED);
    } else if (
      anyApiTestInReportStatus(qualityGateReport, FINISHED_EXCEPTIONALLY)
    ) {
      return qualityGateReport.withReportStatus(FINISHED_EXCEPTIONALLY);
    } else {
      return qualityGateReport.withReportStatus(PASSED);
    }
  }

  private static boolean anyApiTestInReportStatus(
    QualityGateReport qualityGateReport,
    ReportStatus finishedExceptionally
  ) {
    return qualityGateReport
      .getApiTests()
      .parallelStream()
      .anyMatch(apiTest ->
        finishedExceptionally.equals(apiTest.getReportStatus())
      );
  }
}

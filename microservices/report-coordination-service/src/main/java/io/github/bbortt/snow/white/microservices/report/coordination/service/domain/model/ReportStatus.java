package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

public enum ReportStatus {
  IN_PROGRESS,
  FAILED,
  PASSED;

  public static ReportStatus reportStatusFromBooleanValue(
    boolean reportStatus
  ) {
    return reportStatus ? PASSED : FAILED;
  }
}

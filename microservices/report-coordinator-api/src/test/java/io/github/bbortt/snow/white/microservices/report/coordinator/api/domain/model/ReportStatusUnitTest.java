/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FINISHED_EXCEPTIONALLY;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.TIMED_OUT;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.reportStatus;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ReportStatusUnitTest {

  @Nested
  class ReportStatusTest {

    @EnumSource
    @ParameterizedTest
    @VerifiesArch(
      ArchTraceables.ARCH_006_REPORT_STATUS_PERSISTED_AS_STABLE_CODE
    )
    void shouldResolveEveryStatusFromItsOwnCode(ReportStatus reportStatus) {
      assertThat(reportStatus(reportStatus.getVal())).isEqualTo(reportStatus);
    }

    @ValueSource(shorts = { -1, 6, Short.MAX_VALUE })
    @ParameterizedTest
    @VerifiesArch(
      ArchTraceables.ARCH_006_REPORT_STATUS_PERSISTED_AS_STABLE_CODE
    )
    void shouldFallBackToNotStarted_whenCodeIsUnknown(short unknownCode) {
      assertThat(reportStatus(unknownCode)).isEqualTo(NOT_STARTED);
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_006_REPORT_STATUS_PERSISTED_AS_STABLE_CODE
    )
    void shouldAssignStableCodes() {
      // The persisted codes are part of the database contract: pinning them here
      // makes a renumber of the enum fail the build instead of silently changing
      // the meaning of existing rows.
      assertThat(NOT_STARTED.getVal()).isEqualTo((short) 0);
      assertThat(IN_PROGRESS.getVal()).isEqualTo((short) 1);
      assertThat(FAILED.getVal()).isEqualTo((short) 2);
      assertThat(PASSED.getVal()).isEqualTo((short) 3);
      assertThat(FINISHED_EXCEPTIONALLY.getVal()).isEqualTo((short) 4);
      assertThat(TIMED_OUT.getVal()).isEqualTo((short) 5);
    }
  }
}

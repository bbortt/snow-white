/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static java.util.Arrays.stream;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import lombok.Getter;
import org.jspecify.annotations.NonNull;

/**
 * The numeric code per constant is the persisted contract: the enum may be reordered or its
 * constants renamed, but a code must never change meaning.
 */
@Getter
@RealizesArch(ArchTraceables.ARCH_006_REPORT_STATUS_PERSISTED_AS_STABLE_CODE)
public enum ReportStatus {
  NOT_STARTED((short) 0),
  IN_PROGRESS((short) 1),
  FAILED((short) 2),
  PASSED((short) 3),
  FINISHED_EXCEPTIONALLY((short) 4),
  TIMED_OUT((short) 5);

  final short val;

  ReportStatus(short val) {
    this.val = val;
  }

  @RealizesArch(ArchTraceables.ARCH_006_REPORT_STATUS_PERSISTED_AS_STABLE_CODE)
  public static ReportStatus reportStatus(@NonNull Short val) {
    return stream(ReportStatus.values())
      .filter(reportStatus -> reportStatus.getVal() == val)
      .findFirst()
      .orElse(NOT_STARTED);
  }
}

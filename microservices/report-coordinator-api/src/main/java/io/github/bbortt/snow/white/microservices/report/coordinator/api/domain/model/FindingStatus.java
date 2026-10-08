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
 * <p>
 * A code this version does not know decodes to {@code NOT_APPLICABLE} rather than raising. It is
 * the only constant that sits outside both sides of the coverage fraction, so a status added later
 * — {@code WAIVED} being the one already in sight — cannot make an older reader disagree with the
 * ratio stored beside it.
 */
@Getter
@RealizesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
public enum FindingStatus {
  COVERED((short) 0),
  UNCOVERED((short) 1),
  NOT_APPLICABLE((short) 2);

  final short val;

  FindingStatus(short val) {
    this.val = val;
  }

  @RealizesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
  public static FindingStatus findingStatus(@NonNull Short val) {
    return stream(FindingStatus.values())
      .filter(findingStatus -> findingStatus.getVal() == val)
      .findFirst()
      .orElse(NOT_APPLICABLE);
  }
}

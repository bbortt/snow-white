/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.findingStatus;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class FindingStatusUnitTest {

  @Nested
  class FindingStatusTest {

    @EnumSource
    @ParameterizedTest
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    void shouldResolveEveryStatusFromItsOwnCode(FindingStatus findingStatus) {
      assertThat(findingStatus(findingStatus.getVal())).isEqualTo(
        findingStatus
      );
    }

    @ValueSource(shorts = { -1, 3, Short.MAX_VALUE })
    @ParameterizedTest
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    void shouldFallBackToNotApplicable_whenCodeIsUnknown(short unknownCode) {
      // A status this version does not know is one it cannot judge. Decoding it as
      // NOT_APPLICABLE keeps it out of both sides of the fraction, so the row cannot
      // make an older reader disagree with the ratio stored beside it — which is what
      // decoding WAIVED as UNCOVERED would do.
      assertThat(findingStatus(unknownCode)).isEqualTo(NOT_APPLICABLE);
    }

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    void shouldAssignStableCodes() {
      // The persisted codes are part of the database contract: pinning them here
      // makes a renumber of the enum fail the build instead of silently changing
      // the meaning of existing rows.
      assertThat(COVERED.getVal()).isEqualTo((short) 0);
      assertThat(UNCOVERED.getVal()).isEqualTo((short) 1);
      assertThat(NOT_APPLICABLE.getVal()).isEqualTo((short) 2);
    }
  }
}

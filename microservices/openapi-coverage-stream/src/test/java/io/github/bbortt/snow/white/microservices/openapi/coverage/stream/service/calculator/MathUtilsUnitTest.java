/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MathUtilsUnitTest {

  @Nested
  class IntegerApiTest {

    @Test
    void shouldReturn0Coverage_ifCoveredIsZero() {
      assertThat(MathUtils.calculatePercentage(0, 1)).isZero();
    }

    @Test
    void shouldReturn1Coverage_ifRequiredIsZero() {
      assertThat(MathUtils.calculatePercentage(1, 0)).isOne();
    }

    @Test
    void shouldReturnCoverage() {
      assertThat(MathUtils.calculatePercentage(1, 2))
        .isEqualTo(BigDecimal.valueOf(0.5).setScale(2, HALF_UP))
        .hasScaleOf(2);
    }

    @Test
    @VerifiesCon(
      ConTraceables.CON_004_COVERAGE_RATIO_IS_ALWAYS_BOUNDED_AND_WELL_DEFINED
    )
    void shouldNotReportFullCoverage_ifNearlyButNotFullyCovered() {
      assertThat(MathUtils.calculatePercentage(999, 1000))
        .isEqualTo(new BigDecimal("0.99"))
        .isNotEqualTo(BigDecimal.ONE);
    }
  }

  @Nested
  class AtomicLongApiTest {

    @Test
    void shouldReturn0Coverage_ifCoveredIsZero() {
      assertThat(
        MathUtils.calculatePercentage(new AtomicLong(0), new AtomicLong(1))
      ).isZero();
    }

    @Test
    void shouldReturn1Coverage_ifRequiredIsZero() {
      assertThat(
        MathUtils.calculatePercentage(new AtomicLong(1), new AtomicLong(0))
      ).isOne();
    }

    @Test
    void shouldReturnCoverage() {
      assertThat(
        MathUtils.calculatePercentage(new AtomicLong(1), new AtomicLong(2))
      )
        .isEqualTo(BigDecimal.valueOf(0.5).setScale(2, HALF_UP))
        .hasScaleOf(2);
    }

    @Test
    @VerifiesCon(
      ConTraceables.CON_004_COVERAGE_RATIO_IS_ALWAYS_BOUNDED_AND_WELL_DEFINED
    )
    void shouldNotReportFullCoverage_ifNearlyButNotFullyCovered() {
      assertThat(
        MathUtils.calculatePercentage(new AtomicLong(999), new AtomicLong(1000))
      )
        .isEqualTo(new BigDecimal("0.99"))
        .isNotEqualTo(BigDecimal.ONE);
    }
  }
}

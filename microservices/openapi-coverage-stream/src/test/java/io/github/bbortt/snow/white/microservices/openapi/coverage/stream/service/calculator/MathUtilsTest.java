/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MathUtilsTest {

  @Nested
  class IntegerApi {

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
  }

  @Nested
  class AtomicLongApi {

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
  }
}

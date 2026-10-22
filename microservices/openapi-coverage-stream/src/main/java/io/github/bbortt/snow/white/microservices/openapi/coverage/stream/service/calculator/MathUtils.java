/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class MathUtils {

  /**
   * The largest value below {@link BigDecimal#ONE} representable at this class's two-decimal
   * scale — what a near-complete, but not literally complete, ratio is clamped to.
   */
  private static final BigDecimal LARGEST_VALUE_BELOW_ONE = new BigDecimal(
    "0.99"
  );

  static BigDecimal calculatePercentage(int covered, int required) {
    return calculatePercentage(
      new AtomicLong(covered),
      new AtomicLong(required)
    );
  }

  @RealizesCon(
    ConTraceables.CON_004_COVERAGE_RATIO_IS_ALWAYS_BOUNDED_AND_WELL_DEFINED
  )
  static BigDecimal calculatePercentage(
    AtomicLong covered,
    AtomicLong required
  ) {
    if (required.get() == 0) {
      return ONE.setScale(2, HALF_UP);
    } else if (covered.get() == 0) {
      return ZERO.setScale(2, HALF_UP);
    }

    var ratio = new BigDecimal(covered.get()).divide(
      new BigDecimal(required.get()),
      2,
      HALF_UP
    );

    if (covered.get() < required.get() && ratio.compareTo(ONE) == 0) {
      return LARGEST_VALUE_BELOW_ONE;
    }

    return ratio;
  }
}

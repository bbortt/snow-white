/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static lombok.AccessLevel.PRIVATE;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
final class MathUtils {

  static BigDecimal calculatePercentage(int covered, int required) {
    return calculatePercentage(
      new AtomicLong(covered),
      new AtomicLong(required)
    );
  }

  static BigDecimal calculatePercentage(
    AtomicLong covered,
    AtomicLong required
  ) {
    if (required.get() == 0) {
      return ONE.setScale(2, HALF_UP);
    } else if (covered.get() == 0) {
      return ZERO.setScale(2, HALF_UP);
    }

    return new BigDecimal(covered.get()).divide(
      new BigDecimal(required.get()),
      2,
      HALF_UP
    );
  }
}

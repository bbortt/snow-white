/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculatorUtilsTest {

  @Nested
  class getStartedStopWatch {

    @Test
    void shouldReturnStartedStopWatch() {
      var startedStopWatch = CalculatorUtils.getStartedStopWatch();

      assertThat(startedStopWatch)
        .isNotNull()
        .extracting(StopWatch::isStarted)
        .isEqualTo(true);
    }
  }
}

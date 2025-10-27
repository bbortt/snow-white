/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static java.time.Duration.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DurationFormatterTest {

  private DurationFormatter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new DurationFormatter();
  }

  @Nested
  class ToSecondsWithPrecision {

    @Test
    void shouldReturnZeroForZeroDuration() {
      String result = fixture.toSecondsWithPrecision(ZERO);

      assertThat(result).isEqualTo("0");
    }

    @Test
    void shouldFormatSubSecondDurations() {
      String result = fixture.toSecondsWithPrecision(Duration.ofMillis(123));

      assertThat(result).isEqualTo("0.12300");
    }

    @Test
    void shouldFormatOneSecondDuration() {
      String result = fixture.toSecondsWithPrecision(Duration.ofSeconds(1));

      assertThat(result).isEqualTo("1.00000");
    }

    @Test
    void shouldFormatMultipleSecondsWithMillisPrecision() {
      String result = fixture.toSecondsWithPrecision(Duration.ofMillis(3456));

      assertThat(result).isEqualTo("3.45600");
    }

    @Test
    void shouldFormatNanoPrecision() {
      String result = fixture.toSecondsWithPrecision(
        Duration.ofNanos(123456789)
      );

      assertThat(result).isEqualTo("0.12346"); // rounded at 5 decimals
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static java.lang.String.format;

import java.time.Duration;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class DurationFormatter {

  String toSecondsWithPrecision(Duration duration) {
    double seconds = duration.toNanos() / 1_000_000_000.0;

    if (seconds == 0) {
      return "0";
    }

    return format("%.5f", seconds);
  }
}

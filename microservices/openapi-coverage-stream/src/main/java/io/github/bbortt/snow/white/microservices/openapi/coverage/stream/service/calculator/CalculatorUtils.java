/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import org.jspecify.annotations.NonNull;

@NoArgsConstructor(access = PRIVATE)
final class CalculatorUtils {

  static @NonNull StopWatch getStartedStopWatch() {
    var stopWatch = new StopWatch();
    stopWatch.start();
    return stopWatch;
  }
}

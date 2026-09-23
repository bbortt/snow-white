/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

@NoArgsConstructor(access = PRIVATE)
final class SpecPointerUtils {

  private static final String PATHS_POINTER_PREFIX = "/paths/";

  @RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
  static @NonNull String toPathItemPointer(@NonNull String path) {
    return PATHS_POINTER_PREFIX + escape(path);
  }

  /**
   * Escaping order is load-bearing: a literal {@code ~} must become {@code ~0} before any
   * {@code /} becomes {@code ~1}, or the {@code ~} of the latter is escaped a second time.
   */
  private static String escape(String specKey) {
    return specKey.replace("~", "~0").replace("/", "~1");
  }
}

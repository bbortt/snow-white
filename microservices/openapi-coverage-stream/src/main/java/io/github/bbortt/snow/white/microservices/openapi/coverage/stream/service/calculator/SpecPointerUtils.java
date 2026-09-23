/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static java.util.Locale.ROOT;
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
   * The operation node itself, addressed the way the document spells it: OpenAPI keys an
   * operation by its lowercased HTTP method, while an operation key carries it uppercased.
   */
  @RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
  static @NonNull String toOperationPointer(@NonNull String operationKey) {
    return (
      toPathItemPointer(toPath(operationKey)) +
      "/" +
      escape(toMethod(operationKey).toLowerCase(ROOT))
    );
  }

  /**
   * The response entry a response-code criterion judges, one level below the operation's
   * {@code responses} map.
   */
  @RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
  static @NonNull String toResponseEntryPointer(
    @NonNull String operationKey,
    @NonNull String responseCode
  ) {
    return (
      toOperationPointer(operationKey) + "/responses/" + escape(responseCode)
    );
  }

  /**
   * Escaping order is load-bearing: a literal {@code ~} must become {@code ~0} before any
   * {@code /} becomes {@code ~1}, or the {@code ~} of the latter is escaped a second time.
   */
  private static String escape(String specKey) {
    return specKey.replace("~", "~0").replace("/", "~1");
  }
}

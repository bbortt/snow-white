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

  /**
   * The {@code paths} map itself — the shallowest container, and the only node still guaranteed to
   * exist for telemetry the document describes nowhere.
   */
  static final String PATHS_POINTER = "/paths";

  @RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
  static @NonNull String toPathItemPointer(@NonNull String path) {
    return PATHS_POINTER + "/" + escape(path);
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
   * The parameter entry a parameter criterion judges. OpenAPI keys an operation's parameters by
   * list position rather than by name, so this pointer is positional by necessity — the
   * {@code parameterName} discriminator is what still names the target if the array is reordered.
   */
  @RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
  static @NonNull String toParameterPointer(
    @NonNull String operationKey,
    int parameterIndex
  ) {
    return toOperationPointer(operationKey) + "/parameters/" + parameterIndex;
  }

  /**
   * The media-type entry a content-type criterion judges, under the operation's request-body
   * {@code content} map.
   */
  @RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
  static @NonNull String toRequestBodyContentPointer(
    @NonNull String operationKey,
    @NonNull String contentType
  ) {
    return (
      toOperationPointer(operationKey) +
      "/requestBody/content/" +
      escape(contentType)
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
    return toResponsesPointer(operationKey) + "/" + escape(responseCode);
  }

  /**
   * The operation's {@code responses} map, one level above the individual entries — the deepest
   * node an inverted criterion can honestly name, since the code it judges was observed on the
   * wire and may have no entry of its own.
   */
  @RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
  static @NonNull String toResponsesPointer(@NonNull String operationKey) {
    return toOperationPointer(operationKey) + "/responses";
  }

  /**
   * Escaping order is load-bearing: a literal {@code ~} must become {@code ~0} before any
   * {@code /} becomes {@code ~1}, or the {@code ~} of the latter is escaped a second time.
   */
  private static String escape(String specKey) {
    return specKey.replace("~", "~0").replace("/", "~1");
  }
}

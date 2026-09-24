/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.util.Map.entry;
import static java.util.Objects.isNull;
import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.time.StopWatch;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@NoArgsConstructor(access = PRIVATE)
final class CalculatorUtils {

  static @NonNull StopWatch getStartedStopWatch() {
    var stopWatch = new StopWatch();
    stopWatch.start();
    return stopWatch;
  }

  /**
   * Template → concrete direction.
   * Returns all telemetry entries whose operation keys match the given
   * template operation key (which may contain {@code {param}} segments).
   */
  static @NonNull List<OpenTelemetryData> getTelemetryForTemplate(
    @NonNull Map<String, List<OpenTelemetryData>> telemetryMap,
    @NonNull String templateOperationKey
  ) {
    var pattern = OperationKeyCalculator.toOperationKeyPattern(
      templateOperationKey
    );
    return telemetryMap
      .entrySet()
      .stream()
      .filter(e -> pattern.matcher(e.getKey()).matches())
      .flatMap(e -> e.getValue().stream())
      .toList();
  }

  /**
   * Template → concrete direction, ignoring the request method.
   * Returns all telemetry entries whose operation keys resolve to a path matching the given
   * template path (which may contain {@code {param}} segments), under any method.
   */
  static @NonNull List<OpenTelemetryData> getTelemetryForPathTemplate(
    @NonNull Map<String, List<OpenTelemetryData>> telemetryMap,
    @NonNull String templatePath
  ) {
    var pattern = OperationKeyCalculator.toOperationKeyPattern(templatePath);
    return telemetryMap
      .entrySet()
      .stream()
      .filter(e ->
        pattern.matcher(OperationKeyCalculator.toPath(e.getKey())).matches()
      )
      .flatMap(e -> e.getValue().stream())
      .toList();
  }

  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  static @NonNull List<FindingEvidence> toEvidence(
    @NonNull List<OpenTelemetryData> satisfyingTelemetry
  ) {
    return satisfyingTelemetry
      .stream()
      .map(telemetry -> new FindingEvidence(telemetry.traceId(), null))
      .distinct()
      .toList();
  }

  /**
   * Concrete → template direction.
   * Returns the {@link Operation} from the spec map whose template key matches
   * the given concrete operation key (which has resolved path params).
   * Returns {@code null} when no match is found.
   */
  static @Nullable Operation findOperationForConcreteKey(
    @NonNull Map<String, Operation> operationMap,
    @NonNull String concreteOperationKey
  ) {
    var entry = findOperationEntryForConcreteKey(
      operationMap,
      concreteOperationKey
    );
    return isNull(entry) ? null : entry.getValue();
  }

  /**
   * Concrete → template direction, keeping the template key rather than only the
   * {@link Operation} behind it.
   * A finding has to name its target the way the document spells it, so a caller that resolves
   * telemetry back to the spec needs the key that matched, not just what it matched.
   */
  static @Nullable Entry<String, Operation> findOperationEntryForConcreteKey(
    @NonNull Map<String, Operation> operationMap,
    @NonNull String concreteOperationKey
  ) {
    if (operationMap.containsKey(concreteOperationKey)) {
      return entry(
        concreteOperationKey,
        operationMap.get(concreteOperationKey)
      );
    }
    for (Map.Entry<String, Operation> entry : operationMap.entrySet()) {
      var pattern = OperationKeyCalculator.toOperationKeyPattern(
        entry.getKey()
      );
      if (pattern.matcher(concreteOperationKey).matches()) {
        return entry;
      }
    }
    return null;
  }
}

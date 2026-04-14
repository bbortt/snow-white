/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Map;
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
   * Concrete → template direction.
   * Returns the {@link Operation} from the spec map whose template key matches
   * the given concrete operation key (which has resolved path params).
   * Returns {@code null} when no match is found.
   */
  static @Nullable Operation findOperationForConcreteKey(
    @NonNull Map<String, Operation> operationMap,
    @NonNull String concreteOperationKey
  ) {
    if (operationMap.containsKey(concreteOperationKey)) {
      return operationMap.get(concreteOperationKey);
    }
    for (Map.Entry<String, Operation> entry : operationMap.entrySet()) {
      var pattern = OperationKeyCalculator.toOperationKeyPattern(
        entry.getKey()
      );
      if (pattern.matcher(concreteOperationKey).matches()) {
        return entry.getValue();
      }
    }
    return null;
  }
}

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
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Slf4j
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

  /**
   * One entry per distinct {@code (traceId, testCaseName)} pair among the spans the caller's own
   * matching rule attributed to its target, dropped to {@link ApiTestFinding#MAX_EVIDENCE}.
   * <p>
   * The test identity is whatever the span carried, verbatim, and null on a span that carried
   * none - so telemetry without it reduces to exactly the distinct trace ids and adds no
   * cardinality.
   * <p>
   * The bound is applied here, at the one place evidence is produced, because every criterion
   * reaches its target's spans through this method: a cap on any single calculator would leave the
   * other eight unbounded.
   * <p>
   * A drop is reported at {@code DEBUG} rather than as a warning, which is where this diverges from
   * {@code warnAboutUnstorableTestIdentities} and the other bounds in this service. Those report
   * once per calculation about something pathological; this fires for every target a busy window
   * over-evidences, which is routine and would be hundreds of lines per calculation at a level
   * anyone watches. Reporting it once per calculation instead would mean threading an accumulator
   * through all nine calculators - more change than the bound itself.
   */
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  @RealizesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  static @NonNull List<FindingEvidence> toEvidence(
    @NonNull List<OpenTelemetryData> satisfyingTelemetry
  ) {
    var evidence = satisfyingTelemetry
      .stream()
      .map(telemetry ->
        new FindingEvidence(telemetry.traceId(), telemetry.testCaseName())
      )
      .distinct()
      .toList();

    var bounded = ApiTestFinding.bounded(evidence);

    if (bounded.size() < evidence.size()) {
      logger.debug(
        "Bounded evidence for one target from {} entries to {}",
        evidence.size(),
        bounded.size()
      );
    }

    return bounded;
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

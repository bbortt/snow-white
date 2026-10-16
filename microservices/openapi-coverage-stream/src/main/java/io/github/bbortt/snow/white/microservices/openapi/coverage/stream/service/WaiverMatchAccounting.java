/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnmatchedWaiverException;
import java.util.List;
import java.util.Set;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;

/**
 * Checks that every waiver the calculation was dispatched with placed itself somewhere, once all of
 * this API test's criteria have emitted their findings.
 * <p>
 * It runs here rather than inside the waiver pass because the question is about the API test and not
 * about one criterion: a waiver addressing a target only one criterion judges has matched, and a
 * per-criterion check would fail it everywhere else.
 */
@NoArgsConstructor(access = PRIVATE)
@RealizesCon(
  ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
)
final class WaiverMatchAccounting {

  /**
   * Stops the calculation unless every dispatched waiver addressed a target some criterion of this
   * API test emitted a finding for.
   * <p>
   * Matching is on the target alone. A waiver naming a criterion this gate excluded is still
   * matched, because a repository's waiver list is written once and run against whichever gate the
   * pipeline names - requiring every entry to reach a running criterion would fail builds for
   * entries that are merely dormant. The target still has to exist; only the criterion may be
   * absent.
   * <p>
   * Every dispatched waiver is checked, the already-expired ones included: whether a target exists
   * and whether an exemption is still in date are two different questions, and a single combined
   * check would report whichever it tested first - most likely the expiry, hiding the structural
   * problem that outlives it.
   * <p>
   * Because the check runs per API test, a waiver dispatched under a wildcard version has to place
   * itself in every version it reached. That is the intended reading: a wildcard is the only entry
   * whose target can disappear without its author editing anything.
   */
  @RealizesCon(
    ConTraceables.CON_011_WAIVER_MATCHING_NO_FINDING_FAILS_THE_CALCULATION
  )
  static void verifyEveryWaiverMatched(
    @NonNull ApiInformation apiInformation,
    @NonNull Set<OpenApiTestResult> results,
    @NonNull WaiverContext waiverContext
  ) throws UnmatchedWaiverException {
    // What makes an unmatched waiver mean something is that a criterion emits a finding for every
    // target it enumerated, so "nothing matched" can only mean the target is not in the
    // specification as written. A calculation that produced no result at all enumerated nothing,
    // and there the same emptiness means the opposite - no criterion ran - so it is not evidence
    // against any waiver.
    if (waiverContext.isEmpty() || results.isEmpty()) {
      return;
    }

    var findings = results
      .stream()
      .map(OpenApiTestResult::findings)
      .flatMap(List::stream)
      .toList();

    var unmatchedWaivers = waiverContext
      .waivers()
      .stream()
      .filter(waiver -> findings.stream().noneMatch(waiver::addresses))
      .toList();

    if (!unmatchedWaivers.isEmpty()) {
      throw new UnmatchedWaiverException(apiInformation, unmatchedWaivers);
    }
  }
}

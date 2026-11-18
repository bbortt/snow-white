/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Builder
@RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
public record ApiTestFinding(
  @NonNull String specPointer,
  @NonNull FindingStatus status,
  @Nullable String httpPath,
  @Nullable String httpMethod,
  @Nullable String responseCode,
  @Nullable String parameterName,
  @Nullable String contentType,
  @NonNull List<FindingEvidence> evidence,
  @Nullable String waiverReason,
  @Nullable LocalDate waiverExpiresOn
) {
  /**
   * The longest spec pointer a finding is stored under, in UTF-8 bytes.
   * <p>
   * {@code report-coordinator-api} holds it in a {@code VARCHAR(512) NOT NULL} column, so a longer
   * pointer does not cost one finding its identity - the insert fails and the whole calculation's
   * result is lost. In bytes rather than characters for the reason
   * {@link FindingEvidence#MAX_TEST_CASE_NAME_BYTES} records, and stated here rather than in either
   * service because it is a bound both ends have to agree on. Every pointer derived from a document
   * alone is far inside it; the bound exists for the one that appends a value a third party
   * supplied.
   */
  public static final int MAX_SPEC_POINTER_BYTES = 512;

  /**
   * The most evidence one finding carries.
   * <p>
   * Without a bound this is the one part of a coverage response whose size is set by traffic rather
   * than by the document under test: {@code toEvidence} emits an entry per distinct
   * {@code (traceId, testCaseName)} attributed to the target, so a busy window produces thousands
   * for a single finding, across every criterion that judged the same spans. That is what pushes
   * the serialized event past
   * {@link io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent#MAX_SERIALIZED_BYTES}
   * and, before that limit existed, past Kafka's 1 MiB default.
   * <p>
   * Bounded rather than sampled down to one, because the evidence answers two questions and only
   * the first is satisfied by a single entry: that telemetry reached the target at all, and which
   * tests reached it. Thirty-two matches {@link ConformanceVerdict#MAX_VIOLATION_POINTERS} for the
   * same reason it was chosen there - enough detail to act on, not a transcript.
   */
  public static final int MAX_EVIDENCE = 32;

  public ApiTestFinding {
    evidence = List.copyOf(evidence);
  }

  /**
   * The given evidence, dropped to {@link #MAX_EVIDENCE}, admitting the first entry of each
   * distinct test identity before any second entry of one already represented.
   * <p>
   * Diversity first because a blind truncation answers the wrong question: the spans attributed to
   * a target arrive grouped, so the first thirty-two are liable to be thirty-two traces of one test
   * while the other forty tests that covered the target vanish. A reviewer reading a waived
   * finding, or checking which suite exercises an operation, needs the set of tests - not a deeper
   * sample of the first one.
   * <p>
   * Applied where the evidence is produced rather than in the canonical constructor: an event that
   * was serialized before this bound existed has to deserialize whole, and a reader silently
   * dropping what a producer sent would make the record it stored disagree with the record it
   * received.
   */
  public static @NonNull List<FindingEvidence> bounded(
    @NonNull List<FindingEvidence> evidence
  ) {
    if (evidence.size() <= MAX_EVIDENCE) {
      return List.copyOf(evidence);
    }

    var representedTestCases = new HashSet<String>();
    List<FindingEvidence> firstPerTestCase = new ArrayList<>();
    List<FindingEvidence> remainder = new ArrayList<>();

    for (var findingEvidence : evidence) {
      (representedTestCases.add(findingEvidence.testCaseName())
        ? firstPerTestCase
        : remainder
      ).add(findingEvidence);
    }

    firstPerTestCase.addAll(remainder);

    return firstPerTestCase.stream().limit(MAX_EVIDENCE).toList();
  }

  /**
   * This finding as {@code WAIVED}, carrying the justification it was waived under.
   * <p>
   * The justification travels on the finding rather than in a parallel waiver record, because every
   * downstream consumer already walks the findings: the derivation, the report read and the export
   * would otherwise each have to join two collections and agree about the join.
   * <p>
   * The evidence is kept rather than dropped. A waived target that telemetry did reach has evidence
   * worth reading - it is how a reviewer sees that the waiver is no longer needed.
   */
  @RealizesSw(SwTraceables.SW_041_WAIVER_RESOLVES_AGAINST_THE_EMITTED_FINDINGS)
  public ApiTestFinding waivedBecause(
    @NonNull String reason,
    @NonNull LocalDate expiresOn
  ) {
    return new ApiTestFinding(
      specPointer,
      FindingStatus.WAIVED,
      httpPath,
      httpMethod,
      responseCode,
      parameterName,
      contentType,
      evidence,
      reason,
      expiresOn
    );
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import java.time.LocalDate;
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
  public ApiTestFinding {
    evidence = List.copyOf(evidence);
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

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
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
  @NonNull List<FindingEvidence> evidence
) {
  public ApiTestFinding {
    evidence = List.copyOf(evidence);
  }
}

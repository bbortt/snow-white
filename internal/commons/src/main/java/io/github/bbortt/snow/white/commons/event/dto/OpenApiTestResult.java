/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A criterion's outcome as it crosses the service boundary. The findings ride the result they
 * explain rather than a second event, so a persisted ratio never waits for the evidence it has to
 * agree with.
 */
@RealizesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
public record OpenApiTestResult(
  OpenApiCoverageCriteria openApiCriteria,
  BigDecimal coverage,
  Duration duration,
  @Nullable String additionalInformation,
  @JsonSetter(nulls = Nulls.AS_EMPTY) @NonNull List<ApiTestFinding> findings
) {
  /**
   * An event serialized before this field existed deserializes to an empty list rather than
   * failing: a result with a ratio and no evidence for it is the pre-migration record, not a
   * broken one.
   */
  public OpenApiTestResult {
    findings = List.copyOf(findings);
  }

  public OpenApiTestResult(
    OpenApiCoverageCriteria openApiCriteria,
    BigDecimal coverage,
    Duration duration,
    @Nullable String additionalInformation
  ) {
    this(openApiCriteria, coverage, duration, additionalInformation, List.of());
  }

  public OpenApiTestResult(
    OpenApiCoverageCriteria openApiCriteria,
    BigDecimal coverage,
    Duration duration
  ) {
    this(openApiCriteria, coverage, duration, null, List.of());
  }
}

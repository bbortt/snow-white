/*
 *  Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import java.math.BigDecimal;
import java.time.Duration;
import org.jspecify.annotations.Nullable;

public record OpenApiTestResult(
  OpenApiCoverageCriteria openApiCriteria,
  BigDecimal coverage,
  Duration duration,
  @Nullable String additionalInformation
) {
  public OpenApiTestResult(
    OpenApiCoverageCriteria openApiCriteria,
    BigDecimal coverage,
    Duration duration
  ) {
    this(openApiCriteria, coverage, duration, null);
  }
}

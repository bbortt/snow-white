/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record OpenApiCoverageResponseEvent(
  ApiInformation apiInformation,
  @Nullable Set<OpenApiTestResult> openApiTestResults,
  @Nullable String errorMessage
) implements ApiCoverageResponseEvent {
  public OpenApiCoverageResponseEvent(
    ApiInformation apiInformation,
    @NonNull Set<OpenApiTestResult> openApiTestResults
  ) {
    this(apiInformation, openApiTestResults, null);
  }

  public OpenApiCoverageResponseEvent(
    ApiInformation apiInformation,
    @NonNull String errorMessage
  ) {
    this(apiInformation, null, errorMessage);
  }

  @Override
  public ApiType getApiType() {
    return OPENAPI;
  }
}

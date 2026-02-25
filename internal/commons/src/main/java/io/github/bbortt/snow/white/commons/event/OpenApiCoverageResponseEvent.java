/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public record OpenApiCoverageResponseEvent(
  ApiType apiType,
  ApiInformation apiInformation,
  Set<OpenApiTestResult> openApiCriteria,
  @Nullable String error
) implements ApiCoverageResponseEvent {
  public OpenApiCoverageResponseEvent(
    ApiType apiType,
    ApiInformation apiInformation,
    Set<OpenApiTestResult> openApiCriteria
  ) {
    this(apiType, apiInformation, openApiCriteria, null);
  }

  @Override
  public ApiType getApiType() {
    return apiType();
  }
}

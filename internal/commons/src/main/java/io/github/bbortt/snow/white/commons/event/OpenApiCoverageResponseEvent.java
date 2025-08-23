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

public record OpenApiCoverageResponseEvent(
  ApiType apiType,
  ApiInformation apiInformation,
  Set<OpenApiTestResult> openApiCriteria
) implements ApiCoverageResponseEvent {
  @Override
  public ApiType getApiType() {
    return apiType();
  }
}

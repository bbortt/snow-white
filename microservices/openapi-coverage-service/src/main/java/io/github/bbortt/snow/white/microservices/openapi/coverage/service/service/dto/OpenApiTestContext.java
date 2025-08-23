/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.annotation.Nullable;
import java.util.Set;
import lombok.With;

public record OpenApiTestContext(
  ApiInformation apiInformation,
  OpenAPI openAPI,
  String lookbackWindow,
  @With @Nullable Set<OpenTelemetryData> openTelemetryData,
  @With @Nullable Set<OpenApiTestResult> openApiTestResults
) {
  public OpenApiTestContext(
    ApiInformation apiInformation,
    OpenAPI openAPI,
    String lookbackWindow
  ) {
    this(apiInformation, openAPI, lookbackWindow, null, null);
  }
}

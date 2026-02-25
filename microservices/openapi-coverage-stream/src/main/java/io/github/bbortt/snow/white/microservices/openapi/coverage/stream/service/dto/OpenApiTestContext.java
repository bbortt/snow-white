/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Set;
import lombok.With;
import org.jspecify.annotations.Nullable;

public record OpenApiTestContext(
  ApiInformation apiInformation,
  @Nullable OpenAPI openAPI,
  String lookbackWindow,
  Set<FluxAttributeFilter> fluxAttributeFilters,
  @With @Nullable Set<OpenTelemetryData> openTelemetryData,
  @With @Nullable Set<OpenApiTestResult> openApiTestResults,
  @Nullable String e
) {
  public OpenApiTestContext(
    ApiInformation apiInformation,
    OpenAPI openAPI,
    String lookbackWindow,
    Set<FluxAttributeFilter> fluxAttributeFilters
  ) {
    this(
      apiInformation,
      openAPI,
      lookbackWindow,
      fluxAttributeFilters,
      null,
      null,
      null
    );
  }
}

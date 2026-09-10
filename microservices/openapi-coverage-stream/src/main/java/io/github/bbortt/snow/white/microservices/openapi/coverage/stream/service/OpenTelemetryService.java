/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.TelemetryBackendUnavailableException;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

/**
 * Service for fetching telemetry tracing data from an observability backend.
 */
@NullMarked
public interface OpenTelemetryService {
  /**
   * Finds tracing data matching the given API within the requested lookback window.
   *
   * @param apiInformation the API to fetch tracing data for.
   * @param lookbackFromTimestamp the timestamp (epoch millis) to look back from.
   * @param lookbackWindow the lookback window duration.
   * @param attributeFilters optional attribute filters to apply.
   * @return the matching telemetry data.
   * @throws TelemetryBackendUnavailableException if the backend could not be reached, or
   *     responded with a server error, after all retries were exhausted.
   */
  Set<OpenTelemetryData> findOpenTelemetryTracingData(
    ApiInformation apiInformation,
    long lookbackFromTimestamp,
    String lookbackWindow,
    Set<AttributeFilter> attributeFilters
  ) throws TelemetryBackendUnavailableException;
}

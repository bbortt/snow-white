/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Service for calculating OpenAPI coverage.
 */
public interface OpenApiCoverageCalculationService {
  /**
   * Fetches the OpenAPI specification for the given request.
   *
   * @param key the message key.
   * @param calculationRequestEvent the calculation request event.
   * @return the test context, or {@code null} if the specification could not be found or parsed.
   */
  @Nullable
  OpenApiTestContext fetchOpenApiSpecification(
    String key,
    QualityGateCalculationRequestEvent calculationRequestEvent
  );

  /**
   * Enriches the test context with OpenTelemetry data.
   *
   * @param openApiTestContext the test context.
   * @param timestamp the timestamp of the event.
   * @return the enriched test context.
   */
  @NonNull
  OpenApiTestContext enrichWithOpenTelemetryData(
    @NonNull OpenApiTestContext openApiTestContext,
    long timestamp
  );

  /**
   * Calculates the coverage for the given test context.
   *
   * @param openApiTestContext the test context.
   * @return the test context with coverage results.
   */
  @NonNull
  OpenApiTestContext calculateCoverage(
    @NonNull OpenApiTestContext openApiTestContext
  );

  /**
   * Builds the response event for the given test context.
   *
   * @param openApiTestContext the test context.
   * @return the response event.
   */
  @NonNull
  OpenApiCoverageResponseEvent buildResponseEvent(
    @NonNull OpenApiTestContext openApiTestContext
  );
}

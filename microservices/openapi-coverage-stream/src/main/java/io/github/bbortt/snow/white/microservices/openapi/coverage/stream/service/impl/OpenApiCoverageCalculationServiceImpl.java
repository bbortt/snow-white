/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculationService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb.FluxAttributeFilter;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCoverageCalculationServiceImpl
  implements OpenApiCoverageCalculationService
{

  private final OpenApiService openApiService;
  private final OpenTelemetryService openTelemetryService;
  private final OpenApiCoverageService openApiCoverageService;

  @Override
  @WithSpan
  public @NonNull OpenApiTestContext fetchOpenApiSpecification(
    String key,
    QualityGateCalculationRequestEvent calculationRequestEvent
  ) throws OpenApiNotIndexedException, UnparseableOpenApiException {
    return new OpenApiTestContext(
      calculationRequestEvent.getApiInformation(),
      openApiService.findAndParseOpenApi(
        calculationRequestEvent.getApiInformation()
      ),
      calculationRequestEvent.getLookbackWindow(),
      mapToFluxAttributeFilters(calculationRequestEvent.getAttributeFilters())
    );
  }

  @Override
  public @NonNull OpenApiTestContext enrichWithOpenTelemetryData(
    @NonNull OpenApiTestContext openApiTestContext,
    long timestamp
  ) {
    return openApiTestContext.withOpenTelemetryData(
      openTelemetryService.findOpenTelemetryTracingData(
        openApiTestContext.apiInformation(),
        timestamp,
        openApiTestContext.lookbackWindow(),
        openApiTestContext.fluxAttributeFilters()
      )
    );
  }

  @Override
  @WithSpan
  public @NonNull OpenApiTestContext calculateCoverage(
    @NonNull OpenApiTestContext openApiTestContext
  ) {
    return openApiTestContext.withOpenApiTestResults(
      openApiCoverageService.calculateCoverage(openApiTestContext)
    );
  }

  @Override
  public @NonNull OpenApiCoverageResponseEvent buildResponseEvent(
    @NonNull OpenApiTestContext openApiTestContext
  ) {
    return new OpenApiCoverageResponseEvent(
      openApiTestContext.apiInformation(),
      requireNonNull(openApiTestContext.openApiTestResults())
    );
  }

  private static @NonNull Set<FluxAttributeFilter> mapToFluxAttributeFilters(
    Set<AttributeFilter> attributeFilters
  ) {
    return Optional.ofNullable(attributeFilters)
      .orElseGet(Collections::emptySet)
      .stream()
      .map(FluxAttributeFilter::new)
      .collect(toSet());
  }
}

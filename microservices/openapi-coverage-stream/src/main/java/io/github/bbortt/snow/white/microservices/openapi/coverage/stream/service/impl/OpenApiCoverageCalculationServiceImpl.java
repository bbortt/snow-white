/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static java.util.Objects.requireNonNull;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageCalculationService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiCoverageService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenApiService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.OpenTelemetryService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.RequiredAttributeKeyService;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.TelemetryBackendUnavailableException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnmatchedWaiverException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.Collections;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
  private final RequiredAttributeKeyService requiredAttributeKeyService;

  @Override
  @WithSpan
  @RealizesSw(SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT)
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
      Optional.ofNullable(
        calculationRequestEvent.getAttributeFilters()
      ).orElseGet(Collections::emptySet),
      // Both halves come off the event and nowhere else. The instant in particular is the one the
      // trigger pinned - not the record timestamp, whose meaning is a broker setting, and not this
      // pod's clock, which would make the same request answer differently tomorrow.
      new WaiverContext(
        Optional.ofNullable(calculationRequestEvent.getWaivers()).orElseGet(
          Collections::emptyList
        ),
        calculationRequestEvent.getInitiatedAt()
      )
    );
  }

  @Override
  @RealizesArch(
    ArchTraceables.ARCH_007_REQUIRED_ATTRIBUTE_KEYS_COMPUTED_ONCE_BY_CALLER
  )
  public @NonNull OpenApiTestContext enrichWithOpenTelemetryData(
    @NonNull OpenApiTestContext openApiTestContext,
    long timestamp
  ) throws TelemetryBackendUnavailableException {
    return openApiTestContext.withOpenTelemetryData(
      openTelemetryService.findOpenTelemetryTracingData(
        openApiTestContext.apiInformation(),
        timestamp,
        openApiTestContext.lookbackWindow(),
        openApiTestContext.attributeFilters(),
        requiredAttributeKeyService.requiredAttributeKeys(
          openApiTestContext.openAPI()
        )
      )
    );
  }

  @Override
  @WithSpan
  public @NonNull OpenApiTestContext calculateCoverage(
    @NonNull OpenApiTestContext openApiTestContext
  ) throws UnmatchedWaiverException {
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
}

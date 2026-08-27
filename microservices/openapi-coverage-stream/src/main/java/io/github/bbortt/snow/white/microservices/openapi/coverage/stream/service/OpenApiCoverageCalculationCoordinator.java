/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static java.util.Collections.emptySet;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor;
import static java.util.stream.Collectors.toSet;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class OpenApiCoverageCalculationCoordinator {

  private final List<OpenApiCoverageCalculator> openApiCoverageCalculators;

  @WithSpan
  public Set<OpenApiTestResult> calculate(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    if (isEmpty(pathToOpenAPIOperationMap) || isEmpty(pathToTelemetryMap)) {
      return emptySet();
    }

    List<CompletableFuture<OpenApiTestResult>> futures =
      openApiCoverageCalculators
        .stream()
        .map(openApiCoverageCalculator ->
          supplyAsync(
            () ->
              openApiCoverageCalculator.calculate(
                pathToOpenAPIOperationMap,
                pathToTelemetryMap
              ),
            newVirtualThreadPerTaskExecutor()
          )
        )
        .toList();

    return futures.stream().map(CompletableFuture::join).collect(toSet());
  }
}

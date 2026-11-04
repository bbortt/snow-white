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

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.WaiverContext;
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
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap,
    WaiverContext waiverContext
  ) {
    if (
      isEmpty(pathToOpenAPIOperationMap) ||
      isEmpty(pathToTelemetryMap) ||
      onlyHarnessSpansIn(pathToTelemetryMap)
    ) {
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
                pathToTelemetryMap,
                waiverContext
              ),
            newVirtualThreadPerTaskExecutor()
          )
        )
        .toList();

    return futures.stream().map(CompletableFuture::join).collect(toSet());
  }

  /**
   * Whether a harness exported every span of this window, and the application under test none of
   * them.
   * <p>
   * A verdict span is admitted into the analysed dataset so that one criterion can read it, and
   * letting it decide that a calculation runs at all is the largest move it could make on another
   * criterion's verdict: the same window without a producer configured carries no spans and
   * publishes nothing, while with one every criterion that judges reach would publish 0% over a
   * dataset the application never appeared in. So a dataset of verdicts alone stays the
   * no-traffic case it was before this criterion existed.
   */
  @RealizesCon(
    ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT
  )
  private static boolean onlyHarnessSpansIn(
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var spans = pathToTelemetryMap
      .values()
      .stream()
      .flatMap(List::stream)
      .toList();

    return (
      !spans.isEmpty() &&
      spans.stream().allMatch(OpenTelemetryData::isHarnessSpan)
    );
  }
}

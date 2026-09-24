/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForPathTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toPathItemPointer;
import static java.lang.String.format;
import static java.lang.String.join;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Every path defined in the OpenAPI specification has been called.
 * This is a subset of `.{@link OpenApiCoverageCriteria#HTTP_METHOD_COVERAGE}.
 *
 * @see OpenApiCoverageCriteria#PATH_COVERAGE
 */
@Slf4j
@Component
public class PathCoverageCalculator extends AbstractOpenApiCoverageCalculator {

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return PATH_COVERAGE;
  }

  @RealizesSw(SwTraceables.SW_001_STRUCTURAL_CALL_COVERAGE)
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  @Override
  protected @NonNull List<ApiTestFinding> calculateFindings(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    return pathToOpenAPIOperationMap
      .keySet()
      .stream()
      .map(OperationKeyCalculator::toPath)
      .distinct()
      .sorted()
      .map(path -> toFinding(path, pathToTelemetryMap))
      .toList();
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    var uncoveredPaths = calculation
      .findings()
      .stream()
      .filter(finding -> UNCOVERED.equals(finding.status()))
      .map(ApiTestFinding::httpPath)
      .toList();

    if (uncoveredPaths.isEmpty()) {
      return null;
    }

    return format(
      "The following resources (ignoring request methods) are uncovered: `%s`",
      join("`, `", uncoveredPaths)
    );
  }

  private static @NonNull ApiTestFinding toFinding(
    @NonNull String path,
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    List<FindingEvidence> evidence = toEvidence(
      getTelemetryForPathTemplate(pathToTelemetryMap, path)
    );

    if (evidence.isEmpty()) {
      logger.trace("Resource not covered: {}", path);
    } else {
      logger.trace("Resource covered: {}", path);
    }

    return ApiTestFinding.builder()
      .specPointer(toPathItemPointer(path))
      .status(evidence.isEmpty() ? UNCOVERED : COVERED)
      .httpPath(path)
      .evidence(evidence)
      .build();
  }
}

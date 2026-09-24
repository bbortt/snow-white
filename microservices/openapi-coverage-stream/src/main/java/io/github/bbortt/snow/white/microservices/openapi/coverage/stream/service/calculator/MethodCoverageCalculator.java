/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toOperationKey;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toOperationPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.UNCOVERED;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.requireNonNull;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingEvidence;
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
 * Each HTTP method ({@code GET}, {@code POST}, {@code PUT}, {@code DELETE}, etc.) for each path has been tested.
 *
 * @see OpenApiCoverageCriteria#HTTP_METHOD_COVERAGE
 */
@Slf4j
@Component
public class MethodCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return HTTP_METHOD_COVERAGE;
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
      .sorted()
      .map(operationKey -> toFinding(operationKey, pathToTelemetryMap))
      .toList();
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    var uncoveredOperations = calculation
      .findings()
      .stream()
      .filter(finding -> UNCOVERED.equals(finding.status()))
      .map(MethodCoverageCalculator::toUncoveredOperationKey)
      .sorted()
      .toList();

    if (uncoveredOperations.isEmpty()) {
      return null;
    }

    return format(
      "The following paths are uncovered: `%s`",
      join("`, `", uncoveredOperations)
    );
  }

  private static @NonNull ApiTestFinding toFinding(
    @NonNull String operationKey,
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    List<FindingEvidence> evidence = toEvidence(
      getTelemetryForTemplate(pathToTelemetryMap, operationKey)
    );

    if (evidence.isEmpty()) {
      logger.trace("Path not covered: {}", operationKey);
    } else {
      logger.trace("Path covered: {}", operationKey);
    }

    return ApiTestFinding.builder()
      .specPointer(toOperationPointer(operationKey))
      .status(evidence.isEmpty() ? UNCOVERED : COVERED)
      .httpPath(toPath(operationKey))
      .httpMethod(toMethod(operationKey))
      .evidence(evidence)
      .build();
  }

  private static @NonNull String toUncoveredOperationKey(
    @NonNull ApiTestFinding finding
  ) {
    return toOperationKey(
      requireNonNull(finding.httpPath()),
      requireNonNull(finding.httpMethod())
    );
  }
}

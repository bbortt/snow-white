/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.OPERATION_SUCCESS_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toOperationKey;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toOperationPointer;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

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
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each operation has produced at least one successful (2xx) response.
 * <p>
 * This is distinct from {@link OpenApiCoverageCriteria#HTTP_METHOD_COVERAGE}, which only checks that
 * an operation was called at all. This calculator additionally verifies that at least one call
 * resulted in a success response, distinguishing "untested" operations from "called but never
 * succeeded" ones.
 *
 * @see OpenApiCoverageCriteria#OPERATION_SUCCESS_COVERAGE
 */
@Slf4j
@Component
public class OperationSuccessCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return OPERATION_SUCCESS_COVERAGE;
  }

  @RealizesSw(SwTraceables.SW_001_STRUCTURAL_CALL_COVERAGE)
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
    var unsuccessfulOperations = calculation
      .findings()
      .stream()
      .filter(finding -> UNCOVERED.equals(finding.status()))
      .map(OperationSuccessCoverageCalculator::toOperationKeyOf)
      .sorted()
      .toList();

    if (unsuccessfulOperations.isEmpty()) {
      return null;
    }

    return format(
      "The following operations have no successful (2xx) response observed: `%s`",
      join("`, `", unsuccessfulOperations)
    );
  }

  private @NonNull ApiTestFinding toFinding(
    @NonNull String operationKey,
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    List<FindingEvidence> evidence = toEvidence(
      getSuccessfulTelemetry(
        getTelemetryForTemplate(pathToTelemetryMap, operationKey)
      )
    );

    if (evidence.isEmpty()) {
      logger.trace("Operation '{}' has no 2xx response observed", operationKey);
    } else {
      logger.trace(
        "Operation '{}' has at least one 2xx response",
        operationKey
      );
    }

    return ApiTestFinding.builder()
      .specPointer(toOperationPointer(operationKey))
      .status(evidence.isEmpty() ? UNCOVERED : COVERED)
      .httpPath(toPath(operationKey))
      .httpMethod(toMethod(operationKey))
      .evidence(evidence)
      .build();
  }

  /**
   * The spans that satisfied the operation under this criterion's own rule — every one carrying a
   * {@code 2xx} status code, rather than the first of them reduced to a boolean.
   * A non-numeric observed status code is skipped rather than treated as a failure to match.
   */
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  private @NonNull List<OpenTelemetryData> getSuccessfulTelemetry(
    @NonNull List<OpenTelemetryData> telemetryList
  ) {
    return telemetryList.stream().filter(this::isSuccessfulResponse).toList();
  }

  private boolean isSuccessfulResponse(OpenTelemetryData data) {
    if (
      isNull(data.attributes()) ||
      !data.attributes().has(HTTP_RESPONSE_STATUS_CODE.getKey())
    ) {
      return false;
    }

    String statusCode = data
      .attributes()
      .get(HTTP_RESPONSE_STATUS_CODE.getKey())
      .asString();

    try {
      return HttpStatusCode.valueOf(parseInt(statusCode)).is2xxSuccessful();
    } catch (NumberFormatException _) {
      logger.trace(
        "Skipping non-numeric status code '{}' in success check",
        statusCode
      );
      return false;
    }
  }

  private static @NonNull String toOperationKeyOf(
    @NonNull ApiTestFinding finding
  ) {
    return toOperationKey(
      requireNonNull(finding.httpPath()),
      requireNonNull(finding.httpMethod())
    );
  }
}

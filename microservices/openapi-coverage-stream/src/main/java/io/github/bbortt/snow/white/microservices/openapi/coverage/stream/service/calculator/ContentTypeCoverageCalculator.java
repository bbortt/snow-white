/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.CONTENT_TYPE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toOperationKey;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toRequestBodyContentPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.UNCOVERED;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * Calculator for the following criteria:
 * Each documented request body content type for each endpoint has been exercised.
 * Operations without a request body are skipped.
 *
 * @see OpenApiCoverageCriteria#CONTENT_TYPE_COVERAGE
 */
@Slf4j
@Component
public class ContentTypeCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  public static final String CONTENT_TYPE_HEADER_KEY =
    "http.request.header.content-type";

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return CONTENT_TYPE_COVERAGE;
  }

  /**
   * One finding per media type an operation's request body declares.
   * An operation without a request body (or with no content map) declares no media type, so it
   * contributes no target at all rather than an inapplicable one — there is no node in the
   * document for such a finding to point at.
   */
  @RealizesSw(SwTraceables.SW_005_CONTENT_TYPE_COVERAGE)
  @Override
  protected @NonNull List<ApiTestFinding> calculateFindings(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    return pathToOpenAPIOperationMap
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .flatMap(entry ->
        toFindings(
          entry.getKey(),
          entry.getValue(),
          pathToTelemetryMap
        ).stream()
      )
      .toList();
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    var uncoveredContentTypes = calculation
      .findings()
      .stream()
      .filter(finding -> UNCOVERED.equals(finding.status()))
      .map(ContentTypeCoverageCalculator::toUncoveredContentTypeKey)
      .sorted()
      .toList();

    if (uncoveredContentTypes.isEmpty()) {
      return null;
    }

    var message = format(
      "The following request body content types are uncovered: `%s`",
      join("`, `", uncoveredContentTypes)
    );

    if (noContentTypeHeaderObserved(calculation)) {
      message +=
        "\n" +
        "No `content-type` header was observed on any correlated telemetry — header capture may not be enabled (see `OTEL_INSTRUMENTATION_HTTP_SERVER_CAPTURE_REQUEST_HEADERS`, pages/_pages/onboarding.md).";
    }

    return message;
  }

  private @NonNull List<ApiTestFinding> toFindings(
    @NonNull String operationKey,
    @NonNull Operation operation,
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var specContentTypes = extractSpecContentTypes(operation);
    if (specContentTypes.isEmpty()) {
      logger.trace(
        "Operation '{}' has no request body content types defined — skipping",
        operationKey
      );
      return List.of();
    }

    var telemetryList = getTelemetryForTemplate(
      pathToTelemetryMap,
      operationKey
    );

    return specContentTypes
      .stream()
      .map(specContentType ->
        toFinding(operationKey, specContentType, telemetryList)
      )
      .toList();
  }

  private @NonNull ApiTestFinding toFinding(
    @NonNull String operationKey,
    @NonNull String specContentType,
    @NonNull List<OpenTelemetryData> telemetryList
  ) {
    List<FindingEvidence> evidence = toEvidence(
      getSatisfyingTelemetry(specContentType, telemetryList)
    );

    if (evidence.isEmpty()) {
      logger.trace(
        "Content type '{}' NOT covered for operation '{}'",
        specContentType,
        operationKey
      );
    } else {
      logger.trace(
        "Content type '{}' covered for operation '{}'",
        specContentType,
        operationKey
      );
    }

    return ApiTestFinding.builder()
      .specPointer(toRequestBodyContentPointer(operationKey, specContentType))
      .status(evidence.isEmpty() ? UNCOVERED : COVERED)
      .httpPath(toPath(operationKey))
      .httpMethod(toMethod(operationKey))
      .contentType(specContentType)
      .evidence(evidence)
      .build();
  }

  /**
   * The spans that sent this media type. An observed header matches when it starts with the
   * declared value, so a charset or boundary parameter does not prevent a match.
   */
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  private @NonNull List<OpenTelemetryData> getSatisfyingTelemetry(
    @NonNull String specContentType,
    @NonNull List<OpenTelemetryData> telemetryList
  ) {
    return telemetryList
      .stream()
      .filter(telemetryData ->
        extractObservedContentTypes(telemetryData)
          .stream()
          .anyMatch(observed -> observed.startsWith(specContentType))
      )
      .toList();
  }

  private Set<String> extractSpecContentTypes(Operation operation) {
    if (
      isNull(operation.getRequestBody()) ||
      isNull(operation.getRequestBody().getContent())
    ) {
      return Set.of();
    }

    return operation.getRequestBody().getContent().keySet();
  }

  private Set<String> extractObservedContentTypes(
    List<OpenTelemetryData> telemetryList
  ) {
    var observed = new HashSet<String>();

    for (OpenTelemetryData data : telemetryList) {
      observed.addAll(extractObservedContentTypes(data));
    }

    return observed;
  }

  private Set<String> extractObservedContentTypes(OpenTelemetryData data) {
    JsonNode headerNode = isNull(data.attributes())
      ? null
      : data.attributes().get(CONTENT_TYPE_HEADER_KEY);

    if (isNull(headerNode)) {
      return Set.of();
    }

    var observed = new HashSet<String>();

    // OTel may represent header values as a JSON array or a plain string
    if (headerNode.isArray()) {
      headerNode.forEach(element -> observed.add(element.asString()));
    } else {
      observed.add(headerNode.asString());
    }

    return observed;
  }

  /**
   * When none of the telemetry correlated to the evaluated operations carries a
   * {@code content-type} header at all, the hint distinguishes "header capture may not be
   * enabled" from a genuinely untested media type, which would otherwise look identical: a zero
   * score either way.
   * <p>
   * This reads the calculation's input rather than its findings, because the findings cannot tell
   * the two apart: a header that was captured but matched no declared media type leaves every
   * finding uncovered, exactly as a missing header would.
   */
  @RealizesSw(SwTraceables.SW_005_CONTENT_TYPE_COVERAGE)
  private boolean noContentTypeHeaderObserved(
    @NonNull Calculation calculation
  ) {
    var evaluatedTelemetry = calculation
      .pathToOpenAPIOperationMap()
      .entrySet()
      .stream()
      .filter(entry -> !extractSpecContentTypes(entry.getValue()).isEmpty())
      .flatMap(entry ->
        getTelemetryForTemplate(
          calculation.pathToTelemetryMap(),
          entry.getKey()
        ).stream()
      )
      .toList();

    return (
      !evaluatedTelemetry.isEmpty() &&
      extractObservedContentTypes(evaluatedTelemetry).isEmpty()
    );
  }

  private static @NonNull String toUncoveredContentTypeKey(
    @NonNull ApiTestFinding finding
  ) {
    return format(
      "%s [%s]",
      toOperationKey(
        requireNonNull(finding.httpPath()),
        requireNonNull(finding.httpMethod())
      ),
      finding.contentType()
    );
  }
}

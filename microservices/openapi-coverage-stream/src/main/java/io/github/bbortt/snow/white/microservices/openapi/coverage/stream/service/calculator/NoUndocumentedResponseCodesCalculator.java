/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_RESPONSE_CODES;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.findOperationEntryForConcreteKey;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toOperationKey;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.ResponseCodeCoverageCalculator.SINGLE_DIGIT_PATTERN;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.PATHS_POINTER;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toOperationPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toResponsesPointer;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * All response codes (including errors) that occurred must be documented in the OpenAPI specification.
 *
 * @see OpenApiCoverageCriteria#NO_UNDOCUMENTED_RESPONSE_CODES
 */
@Slf4j
@Component
public class NoUndocumentedResponseCodesCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return NO_UNDOCUMENTED_RESPONSE_CODES;
  }

  /**
   * One finding per status code observed on an operation — the inverted direction, where the
   * target space is drawn by the telemetry rather than by the document.
   *
   * <p>An operation with no telemetry contributes nothing, which is why this walks the telemetry
   * and not the spec. Telemetry is resolved back to the operation the document describes, so the
   * several concrete request paths of one templated operation name one target between them: a
   * status code observed twice under {@code /pung/a} and {@code /pung/b} is one question about
   * {@code GET /pung/&#123;message&#125;}, not two.</p>
   */
  @RealizesSw(SwTraceables.SW_003_UNDOCUMENTED_RESPONSE_CODE_DETECTION)
  @Override
  protected @NonNull List<ApiTestFinding> calculateFindings(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    return resolveObservedOperations(
      pathToOpenAPIOperationMap,
      pathToTelemetryMap
    )
      .stream()
      .flatMap(observedOperation -> toFindings(observedOperation).stream())
      .toList();
  }

  /**
   * Every operation the telemetry touched, each carrying the spec operation it resolved to — or
   * none, for telemetry the document describes nowhere.
   */
  private @NonNull List<ObservedOperation> resolveObservedOperations(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    Map<String, ObservedOperation> observedOperations = new HashMap<>();

    for (Map.Entry<String, List<OpenTelemetryData>> entry : sortedByKey(
      pathToTelemetryMap
    )) {
      if (isEmpty(entry.getValue())) {
        continue;
      }

      var specEntry = findOperationEntryForConcreteKey(
        pathToOpenAPIOperationMap,
        entry.getKey()
      );

      var operationKey = isNull(specEntry)
        ? entry.getKey()
        : specEntry.getKey();

      observedOperations
        .computeIfAbsent(operationKey, key ->
          new ObservedOperation(
            key,
            isNull(specEntry) ? null : specEntry.getValue(),
            new ArrayList<>()
          )
        )
        .telemetryData()
        .addAll(entry.getValue());
    }

    return observedOperations
      .values()
      .stream()
      .sorted(comparing(ObservedOperation::operationKey))
      .toList();
  }

  /**
   * Telemetry arrives in an unordered map, and several of its concrete keys can land on one
   * operation — so the order spans are collected in, and with them the order of a finding's
   * evidence, would otherwise vary between two runs over identical input.
   */
  @RealizesCon(ConTraceables.CON_001_DETERMINISTIC_ANALYSIS_RESULTS)
  private static @NonNull List<
    Map.Entry<String, List<OpenTelemetryData>>
  > sortedByKey(
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    return pathToTelemetryMap
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .toList();
  }

  private @NonNull List<ApiTestFinding> toFindings(
    @NonNull ObservedOperation observedOperation
  ) {
    var specifiedResponseCodes = extractSpecifiedResponseCodes(
      observedOperation.operation()
    );
    var specPointer = toSpecPointer(observedOperation);

    return groupTelemetryByObservedResponseCode(
      observedOperation.telemetryData()
    )
      .entrySet()
      .stream()
      .map(entry ->
        toFinding(
          observedOperation,
          specPointer,
          entry.getKey(),
          entry.getValue(),
          specifiedResponseCodes
        )
      )
      .toList();
  }

  /**
   * An observed status code the criterion does not judge is recorded {@code NOT_APPLICABLE}
   * rather than dropped, so a report on error codes still shows the successful ones as something
   * it has nothing to say about.
   */
  @RealizesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
  private @NonNull ApiTestFinding toFinding(
    @NonNull ObservedOperation observedOperation,
    @NonNull String specPointer,
    @NonNull String observedResponseCode,
    @NonNull List<OpenTelemetryData> exhibitingTelemetry,
    @NonNull Set<String> specifiedResponseCodes
  ) {
    var isJudged = judgesObservedResponseCode(observedResponseCode);

    FindingStatus status;
    if (!isJudged) {
      status = NOT_APPLICABLE;
    } else if (
      isResponseCodeDocumented(observedResponseCode, specifiedResponseCodes)
    ) {
      status = COVERED;
    } else {
      status = UNCOVERED;
    }

    logger.trace(
      "Observed response code {} is {} for operation {}",
      observedResponseCode,
      status,
      observedOperation.operationKey()
    );

    return ApiTestFinding.builder()
      .specPointer(specPointer)
      .status(status)
      .httpPath(toPath(observedOperation.operationKey()))
      .httpMethod(toMethod(observedOperation.operationKey()))
      .responseCode(observedResponseCode)
      .evidence(toObservedEvidence(isJudged, exhibitingTelemetry))
      .build();
  }

  /**
   * The traces that exhibited this observed code, attributed whatever the verdict.
   *
   * <p>These criteria run the correlation backwards: the observed span <em>is</em> the target,
   * not the proof that a documented one was reached. So the polarity of evidence flips — an
   * {@code UNCOVERED} finding for an undocumented {@code 418} carries exactly the traces that
   * exhibited it, which is the evidence a developer most needs here. A code the criterion does
   * not judge is attributed nothing, because no rule of its own was applied to it.</p>
   */
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  private static @NonNull List<FindingEvidence> toObservedEvidence(
    boolean isJudged,
    @NonNull List<OpenTelemetryData> exhibitingTelemetry
  ) {
    return isJudged ? toEvidence(exhibitingTelemetry) : emptyList();
  }

  /**
   * The deepest node on the way to the judged code that the document actually contains.
   *
   * <p>An observed code need not be documented at all, so there may be no response entry to point
   * at — and inventing one would read as an assertion that the specification has that entry,
   * which is the precise opposite of what an {@code UNCOVERED} finding here means. The pointer
   * therefore stops at the first node that exists: the operation's {@code responses} map, else
   * the operation node itself where the operation documents no responses, else the {@code paths}
   * map where the document describes the observed operation nowhere. Which code was judged is
   * carried by the {@code responseCode} discriminator either way.</p>
   */
  @RealizesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
  private static @NonNull String toSpecPointer(
    @NonNull ObservedOperation observedOperation
  ) {
    var operation = observedOperation.operation();

    if (isNull(operation)) {
      return PATHS_POINTER;
    }

    if (isNull(operation.getResponses())) {
      return toOperationPointer(observedOperation.operationKey());
    }

    return toResponsesPointer(observedOperation.operationKey());
  }

  private Set<String> extractSpecifiedResponseCodes(
    @Nullable Operation operation
  ) {
    Set<String> specifiedCodes = new HashSet<>();

    if (isNull(operation) || isNull(operation.getResponses())) {
      return specifiedCodes;
    }

    specifiedCodes.addAll(operation.getResponses().keySet());
    return specifiedCodes;
  }

  /**
   * The observed status codes of an operation, each carried together with the spans that
   * exhibited it rather than instead of them — what lets the target also name its evidence.
   * Sorted, so a criterion result states its findings in the same order every run.
   */
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  private static @NonNull Map<
    String,
    List<OpenTelemetryData>
  > groupTelemetryByObservedResponseCode(
    @NonNull List<OpenTelemetryData> telemetryDataList
  ) {
    Map<String, List<OpenTelemetryData>> observedCodes = new TreeMap<>();

    for (OpenTelemetryData telemetryData : telemetryDataList) {
      var statusCode = extractStatusCodeFromAttributes(telemetryData);

      if (nonNull(statusCode)) {
        observedCodes
          .computeIfAbsent(statusCode, code -> new ArrayList<>())
          .add(telemetryData);
      }
    }

    return observedCodes;
  }

  private static @Nullable String extractStatusCodeFromAttributes(
    @NonNull OpenTelemetryData telemetryData
  ) {
    if (
      isNull(telemetryData.attributes()) ||
      !telemetryData.attributes().has(HTTP_RESPONSE_STATUS_CODE.getKey())
    ) {
      return null;
    }

    return telemetryData
      .attributes()
      .get(HTTP_RESPONSE_STATUS_CODE.getKey())
      .asString();
  }

  /**
   * Whether this criterion has anything to say about the given observed status code. This
   * criterion judges every one of them; the narrower ranges are drawn by the subclasses.
   */
  protected boolean judgesObservedResponseCode(
    @NonNull String observedResponseCode
  ) {
    return true;
  }

  private boolean isResponseCodeDocumented(
    String observedCode,
    Set<String> specifiedCodes
  ) {
    // Direct match
    if (specifiedCodes.contains(observedCode)) {
      return true;
    }

    // Check wildcard patterns (e.g., "2XX", "4XX", "5XX")
    for (String specifiedCode : specifiedCodes) {
      if (SINGLE_DIGIT_PATTERN.matcher(specifiedCode).matches()) {
        char wildcardPrefix = specifiedCode.charAt(0);
        if (observedCode.startsWith(String.valueOf(wildcardPrefix))) {
          return true;
        }
      }
    }

    // Check for "default" catch-all
    return (
      specifiedCodes.contains("default") || specifiedCodes.contains("DEFAULT")
    );
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    return getAdditionalInformationOrNull(
      "The following response codes are not documented in the OpenAPI specification: `%s`",
      calculation.findings()
    );
  }

  protected @Nullable String getAdditionalInformationOrNull(
    String messagePattern,
    @NonNull List<ApiTestFinding> findings
  ) {
    var undocumentedCodes = findings
      .stream()
      .filter(finding -> UNCOVERED.equals(finding.status()))
      .map(NoUndocumentedResponseCodesCalculator::toUndocumentedResponseCodeKey)
      .sorted()
      .toList();

    if (undocumentedCodes.isEmpty()) {
      return null;
    }

    return format(messagePattern, join("`, `", undocumentedCodes));
  }

  private static String toUndocumentedResponseCodeKey(ApiTestFinding finding) {
    return format(
      "%s [%s]",
      toOperationKey(
        requireNonNull(finding.httpPath()),
        requireNonNull(finding.httpMethod())
      ),
      finding.responseCode()
    );
  }

  /**
   * An operation the telemetry touched, named the way the document spells it wherever the
   * document spells it at all, together with every span observed on it.
   */
  private record ObservedOperation(
    String operationKey,
    @Nullable Operation operation,
    List<OpenTelemetryData> telemetryData
  ) {}
}

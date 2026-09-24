/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toOperationKey;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toResponseEntryPointer;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static org.springframework.data.util.Predicates.negate;
import static org.springframework.util.CollectionUtils.isEmpty;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria:
 * Each documented response code for each endpoint is tested.
 *
 * @see OpenApiCoverageCriteria#RESPONSE_CODE_COVERAGE
 */
@Slf4j
@Component
public class ResponseCodeCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  public static final Pattern SINGLE_DIGIT_PATTERN = compile("^\\dXX$");

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return RESPONSE_CODE_COVERAGE;
  }

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

  /**
   * One finding per documented response entry of the operation — including the entries this
   * criterion does not judge, which are recorded {@code NOT_APPLICABLE} rather than dropped.
   */
  @RealizesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
  private @NonNull List<ApiTestFinding> toFindings(
    @NonNull String operationKey,
    @NonNull Operation operation,
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var documentedResponseCodes = extractResponseCodes(operation);

    var judgedResponseCodes = documentedResponseCodes
      .stream()
      .filter(responseCode -> judgesResponseCode(responseCode.errorCode()))
      .toList();

    var observedResponseCodes = extractObservedResponseCodes(
      getTelemetryForTemplate(pathToTelemetryMap, operationKey)
    );

    return documentedResponseCodes
      .stream()
      .sorted(comparing(ResponseCode::errorCode))
      .map(responseCode ->
        toFinding(
          operationKey,
          responseCode,
          judgedResponseCodes,
          observedResponseCodes
        )
      )
      .toList();
  }

  private @NonNull ApiTestFinding toFinding(
    @NonNull String operationKey,
    @NonNull ResponseCode responseCode,
    @NonNull List<ResponseCode> judgedResponseCodes,
    @NonNull List<ObservedResponseCode> observedResponseCodes
  ) {
    var errorCode = responseCode.errorCode();
    var isJudged = judgesResponseCode(errorCode);

    List<FindingEvidence> evidence = isJudged
      ? toEvidence(
          getSatisfyingTelemetry(
            responseCode,
            judgedResponseCodes,
            observedResponseCodes
          )
        )
      : emptyList();

    FindingStatus status;
    if (!isJudged) {
      status = NOT_APPLICABLE;
    } else if (evidence.isEmpty()) {
      status = UNCOVERED;
    } else {
      status = COVERED;
    }

    logger.trace(
      "Response code {} is {} for operation {}",
      errorCode,
      status,
      operationKey
    );

    return ApiTestFinding.builder()
      .specPointer(toResponseEntryPointer(operationKey, errorCode))
      .status(status)
      .httpPath(toPath(operationKey))
      .httpMethod(toMethod(operationKey))
      .responseCode(errorCode)
      .evidence(evidence)
      .build();
  }

  /**
   * The telemetry that satisfied this response entry under this criterion's own rule.
   *
   * <p>A documented {@code default} entry is a catch-all, not a literal value that can appear on
   * the wire: it is satisfied by every observed status code that no other, more specific entry of
   * the same operation already matches. Every other entry is matched by its own pattern.</p>
   */
  @RealizesSw(
    SwTraceables.SW_002_RESPONSE_CODE_COVERAGE_TREATS_DEFAULT_AS_WILDCARD
  )
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  private List<OpenTelemetryData> getSatisfyingTelemetry(
    ResponseCode responseCode,
    List<ResponseCode> judgedResponseCodes,
    List<ObservedResponseCode> observedResponseCodes
  ) {
    if (!isDefaultResponseCode(responseCode.errorCode())) {
      var isMatch = responseCode.errorCodePattern().asPredicate();
      return observedResponseCodes
        .stream()
        .filter(observed -> isMatch.test(observed.statusCode()))
        .map(ObservedResponseCode::telemetryData)
        .toList();
    }

    var otherPatterns = judgedResponseCodes
      .stream()
      .filter(other -> other != responseCode)
      .map(ResponseCode::errorCodePattern)
      .toList();

    return observedResponseCodes
      .stream()
      .filter(observed ->
        otherPatterns
          .stream()
          .noneMatch(pattern ->
            pattern.matcher(observed.statusCode()).matches()
          )
      )
      .map(ObservedResponseCode::telemetryData)
      .toList();
  }

  private static boolean isDefaultResponseCode(String errorCode) {
    return "default".equalsIgnoreCase(errorCode);
  }

  /**
   * Every response entry the operation documents, in the order the document states them.
   */
  protected List<ResponseCode> extractResponseCodes(Operation operation) {
    List<ResponseCode> responseCodes = new ArrayList<>();

    if (isNull(operation.getResponses())) {
      return responseCodes;
    }

    for (Map.Entry<String, ApiResponse> responseEntry : operation
      .getResponses()
      .entrySet()) {
      String statusCode = responseEntry.getKey();

      if (isResponseCodePattern(statusCode)) {
        responseCodes.add(
          new ResponseCode(
            statusCode,
            compile(format("^%s\\d\\d$", statusCode.charAt(0)))
          )
        );
      } else {
        responseCodes.add(
          new ResponseCode(statusCode, compile(format("^%s$", statusCode)))
        );
      }
    }

    return responseCodes;
  }

  private List<ObservedResponseCode> extractObservedResponseCodes(
    @Nullable List<OpenTelemetryData> telemetryDataList
  ) {
    List<ObservedResponseCode> observedCodes = new ArrayList<>();

    if (isEmpty(telemetryDataList)) {
      return observedCodes;
    }

    for (OpenTelemetryData telemetryData : telemetryDataList) {
      var statusCode = extractStatusCodeFromAttributes(telemetryData);
      if (judgesResponseCode(statusCode)) {
        observedCodes.add(new ObservedResponseCode(statusCode, telemetryData));
      }
    }

    return observedCodes;
  }

  /**
   * Whether this criterion has anything to say about the given response code — asked of a
   * documented entry to decide whether it is a target at all, and of an observed status code to
   * decide whether it can satisfy one. This criterion judges every documented entry; the narrower
   * subsets are drawn by the subclasses.
   */
  protected boolean judgesResponseCode(@Nullable String statusCode) {
    return nonNull(statusCode);
  }

  private static @Nullable String extractStatusCodeFromAttributes(
    OpenTelemetryData telemetryData
  ) {
    if (isNull(telemetryData.attributes())) {
      return null;
    }

    var attributes = telemetryData.attributes();
    if (attributes.has(HTTP_RESPONSE_STATUS_CODE.getKey())) {
      return attributes.get(HTTP_RESPONSE_STATUS_CODE.getKey()).asString();
    }

    logger.debug(
      "No HTTP status code found in telemetry attributes for span {}",
      telemetryData.spanId()
    );

    return null;
  }

  /**
   * Detects OpenAPI response code patterns like "4XX", "5XX", "default".
   */
  protected static boolean isResponseCodePattern(@Nullable String statusCode) {
    return (
      nonNull(statusCode) &&
      !statusCode.isEmpty() &&
      SINGLE_DIGIT_PATTERN.matcher(statusCode).matches()
    );
  }

  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    return getAdditionalInformationOrNull(
      "The following response codes in paths are uncovered: `%s`",
      calculation.findings()
    );
  }

  protected @Nullable String getAdditionalInformationOrNull(
    String infoMessagePattern,
    @NonNull List<ApiTestFinding> findings
  ) {
    var uncoveredResponseCodes = findings
      .stream()
      .filter(finding -> UNCOVERED.equals(finding.status()))
      .map(ResponseCodeCoverageCalculator::toUncoveredResponseCodeKey)
      .toList();

    if (uncoveredResponseCodes.isEmpty()) {
      return null;
    }

    var codes = uncoveredResponseCodes
      .stream()
      .filter(negate(ResponseCodeCoverageCalculator::isDefaultCodePattern))
      .toList();

    var additionalInformationBuilder = new StringBuilder();

    if (!codes.isEmpty()) {
      additionalInformationBuilder.append(
        format(
          infoMessagePattern,
          join("`, `", codes.stream().sorted().toList())
        )
      );
    }

    var defaultCodes = uncoveredResponseCodes
      .stream()
      .filter(ResponseCodeCoverageCalculator::isDefaultCodePattern)
      .toList();

    if (!codes.isEmpty() && !defaultCodes.isEmpty()) {
      additionalInformationBuilder.append(lineSeparator());
    }

    if (!defaultCodes.isEmpty()) {
      additionalInformationBuilder.append(
        format(
          "The following default response codes in paths are uncovered, as no observed status code fell outside a more specific documented response: `%s`",
          join("`, `", defaultCodes.stream().sorted().toList())
        )
      );
    }

    return additionalInformationBuilder.toString();
  }

  private static String toUncoveredResponseCodeKey(ApiTestFinding finding) {
    return format(
      "%s [%s]",
      toOperationKey(
        requireNonNull(finding.httpPath()),
        requireNonNull(finding.httpMethod())
      ),
      finding.responseCode()
    );
  }

  private static boolean isDefaultCodePattern(String errorCode) {
    return errorCode.endsWith("[default]");
  }

  protected record ResponseCode(String errorCode, Pattern errorCodePattern) {}

  /**
   * An observed status code carried together with the span it came from, rather than instead of
   * it — what lets the match that proves a target also name its evidence.
   */
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  private record ObservedResponseCode(
    String statusCode,
    OpenTelemetryData telemetryData
  ) {}
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.toEvidence;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.HttpStatusCodeUtils.isErrorHttpStatusCode;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toMethod;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toPath;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.SpecPointerUtils.toResponseEntryPointer;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.UNCOVERED;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Locale.ROOT;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingEvidence;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Calculator for the following criteria: Error responses include all required fields.
 * <p>
 * This calculator verifies that the OpenAPI specification defines required fields for error responses and that error responses observed in telemetry match the expected error response patterns.
 * <p>
 * Note: Due to OpenTelemetry's limited visibility into response bodies, this calculator primarily validates that error responses have been observed for operations that define required error fields in their schema.
 * Full field-level validation would require additional instrumentation.
 *
 * @see OpenApiCoverageCriteria#REQUIRED_ERROR_FIELDS_COVERAGE
 */
@Slf4j
@Component
public class RequiredErrorFieldsCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  /**
   * One finding per documented response entry of the operation.
   * A positive entry, and an error entry whose schema declares no required field, are targets this
   * criterion has nothing to say about: they are recorded {@code NOT_APPLICABLE} rather than
   * dropped, and enter neither side of the fraction.
   */
  @RealizesSw(SwTraceables.SW_006_REQUIRED_ERROR_FIELDS_COVERAGE)
  @RealizesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
  @Override
  protected @NonNull List<ApiTestFinding> calculateFindings(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    return toResponseTargets(pathToOpenAPIOperationMap)
      .stream()
      .map(target -> toFinding(target, pathToTelemetryMap))
      .toList();
  }

  @Override
  protected @NonNull OpenApiCoverageCriteria getSupportedOpenApiCoverageCriteria() {
    return REQUIRED_ERROR_FIELDS_COVERAGE;
  }

  /**
   * The required fields belong in the message but are not one of the finding's discriminators, so
   * the label is built by re-walking the same enumeration the findings came from and matching each
   * target on the pointer it produced.
   */
  @Override
  protected @Nullable String getAdditionalInformationOrNull(
    @NonNull Calculation calculation
  ) {
    var uncoveredPointers = calculation
      .findings()
      .stream()
      .filter(finding -> UNCOVERED.equals(finding.status()))
      .map(ApiTestFinding::specPointer)
      .collect(toSet());

    if (uncoveredPointers.isEmpty()) {
      return null;
    }

    var uncoveredSchemas = toResponseTargets(
      calculation.pathToOpenAPIOperationMap()
    )
      .stream()
      .filter(target -> uncoveredPointers.contains(target.specPointer()))
      .map(RequiredErrorFieldsCoverageCalculator::toUncoveredSchemaLabel)
      .distinct()
      .sorted()
      .toList();

    return format(
      "The following error responses with required fields are not covered: `%s`",
      join("`, `", uncoveredSchemas)
    );
  }

  /**
   * Every documented response entry of every operation, in document order, each carrying the
   * required fields its schema declares — an empty set where it declares none.
   */
  private static @NonNull List<ResponseTarget> toResponseTargets(
    @NonNull Map<String, Operation> pathToOpenAPIOperationMap
  ) {
    var targets = new ArrayList<ResponseTarget>();

    pathToOpenAPIOperationMap
      .entrySet()
      .stream()
      .sorted(Map.Entry.comparingByKey())
      .forEach(entry -> {
        var responses = entry.getValue().getResponses();

        if (isNull(responses)) {
          return;
        }

        responses.forEach((statusCode, response) ->
          targets.add(
            new ResponseTarget(
              entry.getKey(),
              statusCode,
              extractRequiredFieldsFromResponse(response)
            )
          )
        );
      });

    return targets;
  }

  private @NonNull ApiTestFinding toFinding(
    @NonNull ResponseTarget target,
    @NonNull Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var isJudged = target.isJudged();

    List<FindingEvidence> evidence = isJudged
      ? toEvidence(
          getSatisfyingTelemetry(
            target.responseCode(),
            pathToTelemetryMap.get(target.operationKey())
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
      "Error response '{}' with required fields {} is {} in operation '{}'",
      target.responseCode(),
      target.requiredFields(),
      status,
      target.operationKey()
    );

    return ApiTestFinding.builder()
      .specPointer(target.specPointer())
      .status(status)
      .httpPath(toPath(target.operationKey()))
      .httpMethod(toMethod(target.operationKey()))
      .responseCode(target.responseCode())
      .evidence(evidence)
      .build();
  }

  /**
   * The spans that exhibited this documented error response — an exact match for a literal code, a
   * shared leading digit for a wildcard pattern, and any observed error code at all for a
   * {@code default} entry, which this criterion classifies as an error.
   *
   * <p>A {@code default} entry is not evidenced by exclusion here: any observed error code covers
   * it, including one a more specific sibling entry already matched.</p>
   */
  @RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  @RealizesSw(SwTraceables.SW_006_REQUIRED_ERROR_FIELDS_COVERAGE)
  private @NonNull List<OpenTelemetryData> getSatisfyingTelemetry(
    @NonNull String specifiedErrorCode,
    @Nullable List<OpenTelemetryData> telemetryDataList
  ) {
    if (isNull(telemetryDataList)) {
      return emptyList();
    }

    return telemetryDataList
      .stream()
      .filter(telemetryData -> {
        var observedErrorCode = extractObservedErrorCode(telemetryData);
        return (
          nonNull(observedErrorCode) &&
          matchesObservedErrorCode(specifiedErrorCode, observedErrorCode)
        );
      })
      .toList();
  }

  private boolean matchesObservedErrorCode(
    @NonNull String specifiedErrorCode,
    @NonNull String observedErrorCode
  ) {
    // Direct match
    if (observedErrorCode.equals(specifiedErrorCode)) {
      return true;
    }

    String upperCode = specifiedErrorCode.toUpperCase(ROOT);

    // Pattern match (e.g., "4XX" matches "400", "404", etc.)
    if (upperCode.endsWith("XX")) {
      return observedErrorCode.startsWith(String.valueOf(upperCode.charAt(0)));
    }

    // Default catches all errors
    return upperCode.equals("DEFAULT");
  }

  private static @Nullable String extractObservedErrorCode(
    @NonNull OpenTelemetryData telemetryData
  ) {
    if (
      isNull(telemetryData.attributes()) ||
      !telemetryData.attributes().has(HTTP_RESPONSE_STATUS_CODE.getKey())
    ) {
      return null;
    }

    String statusCode = telemetryData
      .attributes()
      .get(HTTP_RESPONSE_STATUS_CODE.getKey())
      .asString();

    return nonNull(statusCode) && isErrorHttpStatusCode(statusCode)
      ? statusCode
      : null;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private static Set<String> extractRequiredFieldsFromResponse(
    ApiResponse response
  ) {
    Set<String> requiredFields = new HashSet<>();

    Content content = response.getContent();
    if (isNull(content)) {
      return requiredFields;
    }

    for (MediaType mediaType : content.values()) {
      Schema schema = mediaType.getSchema();
      if (nonNull(schema) && nonNull(schema.getRequired())) {
        requiredFields.addAll(schema.getRequired());
      }
    }

    return requiredFields;
  }

  private static String toUncoveredSchemaLabel(ResponseTarget target) {
    return format(
      "%s [%s: fields=%s]",
      target.operationKey(),
      target.responseCode(),
      join(", ", target.requiredFields())
    );
  }

  /**
   * A documented response entry together with what names it and what this criterion needs of it:
   * the operation it belongs to, its status-code key, and the required fields its schema declares.
   */
  private record ResponseTarget(
    String operationKey,
    String responseCode,
    Set<String> requiredFields
  ) {
    String specPointer() {
      return toResponseEntryPointer(operationKey, responseCode);
    }

    boolean isJudged() {
      return isErrorHttpStatusCode(responseCode) && !requiredFields.isEmpty();
    }
  }
}

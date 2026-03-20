/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.HttpStatusCodeUtils.isErrorHttpStatusCode;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
 * @see OpenApiCriteria#REQUIRED_ERROR_FIELDS_COVERAGE
 */
@Slf4j
@Component
public class RequiredErrorFieldsCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return REQUIRED_ERROR_FIELDS_COVERAGE;
  }

  @Override
  protected @NonNull CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var totalErrorResponsesWithRequiredFields = new AtomicInteger(0);
    var coveredErrorResponses = new AtomicInteger(0);

    var uncoveredErrorSchemas = new HashSet<String>();

    for (Map.Entry<
      String,
      Operation
    > entry : pathToOpenAPIOperationMap.entrySet()) {
      String operationKey = entry.getKey();
      Operation operation = entry.getValue();

      var errorResponsesWithRequiredFields =
        extractErrorResponsesWithRequiredFields(operation);

      if (errorResponsesWithRequiredFields.isEmpty()) {
        logger.trace(
          "Operation '{}' has no error responses with required fields",
          operationKey
        );
        continue;
      }

      var telemetryDataList = pathToTelemetryMap.get(operationKey);
      Set<String> observedErrorCodes = extractObservedErrorCodes(
        telemetryDataList
      );

      for (Map.Entry<
        String,
        Set<String>
      > errorEntry : errorResponsesWithRequiredFields.entrySet()) {
        String errorCode = errorEntry.getKey();
        Set<String> requiredFields = errorEntry.getValue();

        totalErrorResponsesWithRequiredFields.incrementAndGet();

        if (isErrorResponseCovered(errorCode, observedErrorCodes)) {
          logger.trace(
            "Error response '{}' with required fields {} covered in operation '{}'",
            errorCode,
            requiredFields,
            operationKey
          );
          coveredErrorResponses.incrementAndGet();
        } else {
          logger.trace(
            "Error response '{}' with required fields {} NOT covered in operation '{}'",
            errorCode,
            requiredFields,
            operationKey
          );
          uncoveredErrorSchemas.add(
            format(
              "%s [%s: fields=%s]",
              operationKey,
              errorCode,
              join(", ", requiredFields)
            )
          );
        }
      }
    }

    var coverage = calculatePercentage(
      coveredErrorResponses.get(),
      totalErrorResponsesWithRequiredFields.get()
    );

    return new CoverageCalculationResult(
      coverage,
      getAdditionalInformationOrNull(uncoveredErrorSchemas)
    );
  }

  private Map<String, Set<String>> extractErrorResponsesWithRequiredFields(
    Operation operation
  ) {
    var errorResponses = new java.util.HashMap<String, Set<String>>();

    if (isNull(operation.getResponses())) {
      return errorResponses;
    }

    for (Map.Entry<String, ApiResponse> entry : operation
      .getResponses()
      .entrySet()) {
      String statusCode = entry.getKey();
      ApiResponse response = entry.getValue();

      if (!isErrorHttpStatusCode(statusCode)) {
        continue;
      }

      Set<String> requiredFields = extractRequiredFieldsFromResponse(response);
      if (!requiredFields.isEmpty()) {
        errorResponses.put(statusCode, requiredFields);
      }
    }

    return errorResponses;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Set<String> extractRequiredFieldsFromResponse(ApiResponse response) {
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

  private Set<String> extractObservedErrorCodes(
    @Nullable List<OpenTelemetryData> telemetryDataList
  ) {
    Set<String> observedErrorCodes = new HashSet<>();

    if (isNull(telemetryDataList)) {
      return observedErrorCodes;
    }

    for (OpenTelemetryData telemetryData : telemetryDataList) {
      if (
        isNull(telemetryData.attributes()) ||
        !telemetryData.attributes().has(HTTP_RESPONSE_STATUS_CODE.getKey())
      ) {
        continue;
      }

      String statusCode = telemetryData
        .attributes()
        .get(HTTP_RESPONSE_STATUS_CODE.getKey())
        .asString();

      if (nonNull(statusCode) && isErrorHttpStatusCode(statusCode)) {
        observedErrorCodes.add(statusCode);
      }
    }

    return observedErrorCodes;
  }

  private boolean isErrorResponseCovered(
    String specifiedErrorCode,
    Set<String> observedErrorCodes
  ) {
    // Direct match
    if (observedErrorCodes.contains(specifiedErrorCode)) {
      return true;
    }

    // Pattern match (e.g., "4XX" matches "400", "404", etc.)
    String upperCode = specifiedErrorCode.toUpperCase();
    if (upperCode.endsWith("XX")) {
      char prefix = upperCode.charAt(0);
      for (String observed : observedErrorCodes) {
        if (observed.startsWith(String.valueOf(prefix))) {
          return true;
        }
      }
    }

    // Default catches all errors
    if (upperCode.equals("DEFAULT")) {
      return !observedErrorCodes.isEmpty();
    }

    return false;
  }

  private @Nullable String getAdditionalInformationOrNull(
    @NonNull Set<String> uncoveredSchemas
  ) {
    if (uncoveredSchemas.isEmpty()) {
      return null;
    }

    var sortedSchemas = uncoveredSchemas.stream().sorted().toList();

    return format(
      "The following error responses with required fields are not covered: `%s`",
      join("`, `", sortedSchemas)
    );
  }
}

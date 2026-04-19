/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.CONTENT_TYPE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.CalculatorUtils.getTelemetryForTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.MathUtils.calculatePercentage;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Objects.isNull;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
 * @see OpenApiCriteria#CONTENT_TYPE_COVERAGE
 */
@Slf4j
@Component
public class ContentTypeCoverageCalculator
  extends AbstractOpenApiCoverageCalculator
{

  static final String CONTENT_TYPE_HEADER_KEY =
    "http.request.header.content-type";

  @Override
  protected @NonNull OpenApiCriteria getSupportedOpenApiCriteria() {
    return CONTENT_TYPE_COVERAGE;
  }

  @Override
  protected @NonNull CoverageCalculationResult calculateCoverage(
    Map<String, Operation> pathToOpenAPIOperationMap,
    Map<String, List<OpenTelemetryData>> pathToTelemetryMap
  ) {
    var coveredContentTypes = new AtomicInteger(0);
    var totalContentTypes = new AtomicInteger(0);

    var uncoveredContentTypes = new HashSet<String>();

    for (Map.Entry<
      String,
      Operation
    > entry : pathToOpenAPIOperationMap.entrySet()) {
      String operationKey = entry.getKey();
      Operation operation = entry.getValue();

      var specContentTypes = extractSpecContentTypes(operation);
      if (specContentTypes.isEmpty()) {
        logger.trace(
          "Operation '{}' has no request body content types defined — skipping",
          operationKey
        );
        continue;
      }

      totalContentTypes.addAndGet(specContentTypes.size());

      var telemetryList = getTelemetryForTemplate(
        pathToTelemetryMap,
        operationKey
      );
      var observedContentTypes = extractObservedContentTypes(telemetryList);

      for (String specContentType : specContentTypes) {
        if (isContentTypeCovered(specContentType, observedContentTypes)) {
          logger.trace(
            "Content type '{}' covered for operation '{}'",
            specContentType,
            operationKey
          );
          coveredContentTypes.incrementAndGet();
        } else {
          logger.trace(
            "Content type '{}' NOT covered for operation '{}'",
            specContentType,
            operationKey
          );
          uncoveredContentTypes.add(
            format("%s [%s]", operationKey, specContentType)
          );
        }
      }
    }

    var coverage = calculatePercentage(
      coveredContentTypes.get(),
      totalContentTypes.get()
    );

    return new CoverageCalculationResult(
      coverage,
      getAdditionalInformationOrNull(uncoveredContentTypes)
    );
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
      if (isNull(data.attributes())) {
        continue;
      }

      JsonNode headerNode = data.attributes().get(CONTENT_TYPE_HEADER_KEY);
      if (isNull(headerNode)) {
        continue;
      }

      // OTel may represent header values as a JSON array or a plain string
      if (headerNode.isArray()) {
        headerNode.forEach(element -> observed.add(element.asString()));
      } else {
        observed.add(headerNode.asString());
      }
    }

    return observed;
  }

  /**
   * Checks whether {@code specContentType} is covered by any observed value.
   * An observed value matches when it starts with the spec content type, allowing
   * for parameters such as {@code ; charset=utf-8}.
   */
  private boolean isContentTypeCovered(
    String specContentType,
    Set<String> observedContentTypes
  ) {
    for (String observed : observedContentTypes) {
      if (observed.startsWith(specContentType)) {
        return true;
      }
    }
    return false;
  }

  private @Nullable String getAdditionalInformationOrNull(
    @NonNull Set<String> uncoveredContentTypes
  ) {
    if (uncoveredContentTypes.isEmpty()) {
      return null;
    }

    var sorted = uncoveredContentTypes.stream().sorted().toList();

    return format(
      "The following request body content types are uncovered: `%s`",
      join("`, `", sorted)
    );
  }
}

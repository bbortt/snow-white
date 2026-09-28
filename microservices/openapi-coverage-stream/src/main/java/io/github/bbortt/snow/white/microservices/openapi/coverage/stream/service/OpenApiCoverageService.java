/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.OperationKeyCalculator.toOperationKey;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.DELETE;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.HEAD;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.OPTIONS;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PATCH;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.POST;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;
import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCoverageService {

  private static final Set<PathItemMapping> METHOD_ACCESSORS = Set.of(
    new PathItemMapping(PathItem::getGet, GET),
    new PathItemMapping(PathItem::getPost, POST),
    new PathItemMapping(PathItem::getPut, PUT),
    new PathItemMapping(PathItem::getDelete, DELETE),
    new PathItemMapping(PathItem::getPatch, PATCH),
    new PathItemMapping(PathItem::getHead, HEAD),
    new PathItemMapping(PathItem::getOptions, OPTIONS)
  );

  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;
  private final OpenApiCoverageCalculationCoordinator openApiCoverageCalculationCoordinator;

  @WithSpan
  public Set<OpenApiTestResult> calculateCoverage(
    @NonNull OpenApiTestContext openApiTestContext
  ) {
    if (isEmpty(openApiTestContext.openTelemetryData())) {
      return emptySet();
    }

    return calculateCoverage(
      openApiTestContext.openAPI(),
      openApiTestContext.openTelemetryData()
    );
  }

  private Set<OpenApiTestResult> calculateCoverage(
    OpenAPI openApi,
    Set<OpenTelemetryData> openTelemetryData
  ) {
    logger.info(
      "Calculating OpenAPI coverage for {} telemetry data points",
      openTelemetryData.size()
    );

    var pathIndex = buildPathIndex(openApi);
    var pathToTelemetryMap = groupTelemetryByPath(
      openTelemetryData,
      pathIndex.operationIdToOperationKey()
    );

    return openApiCoverageCalculationCoordinator.calculate(
      pathIndex.operationKeyToOperation(),
      pathToTelemetryMap
    );
  }

  private OpenApiPathIndex buildPathIndex(OpenAPI openApi) {
    Map<String, Operation> operationKeyToOperation = new HashMap<>();
    Map<String, String> operationIdToOperationKey = new HashMap<>();

    if (isEmpty(openApi.getPaths())) {
      return new OpenApiPathIndex(
        operationKeyToOperation,
        operationIdToOperationKey
      );
    }

    openApi.getPaths().forEach((path, pathItem) -> {
      for (var pathItemMapping : METHOD_ACCESSORS) {
        var operation = pathItemMapping.mappingFunction().apply(pathItem);
        if (nonNull(operation)) {
          var operationKey = toOperationKey(
            path,
            pathItemMapping.httpMethodString()
          );
          operationKeyToOperation.put(operationKey, operation);
          if (hasText(operation.getOperationId())) {
            operationIdToOperationKey.put(
              operation.getOperationId(),
              operationKey
            );
          }
        }
      }
    });

    return new OpenApiPathIndex(
      operationKeyToOperation,
      operationIdToOperationKey
    );
  }

  /**
   * Groups the telemetry by the operation it belongs to, and hoists each span's test identity on
   * the way past.
   * <p>
   * This is the one place that knows which attribute the operator configured for a test identity,
   * so it is the one place that reads it: a calculator is handed spans that already name their
   * test, and stays free of configuration it would only pass on. Hoisting here also keeps the
   * identity out of the grouping key - the attribute never influences which operation a span is
   * attributed to.
   */
  @RealizesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  private Map<String, List<OpenTelemetryData>> groupTelemetryByPath(
    Set<OpenTelemetryData> telemetryData,
    Map<String, String> operationIdToOperationKey
  ) {
    var operationIdAttr =
      openApiCoverageStreamProperties.getOperationIdAttribute();
    var testCaseNameAttr =
      openApiCoverageStreamProperties.getTestCaseNameAttribute();
    return telemetryData
      .stream()
      .filter(data ->
        isRoutable(data, operationIdAttr, operationIdToOperationKey)
      )
      .collect(
        groupingBy(
          data ->
            resolveOperationKey(
              data,
              operationIdAttr,
              operationIdToOperationKey
            ),
          mapping(data -> data.withTestIdentityFrom(testCaseNameAttr), toList())
        )
      );
  }

  private boolean isRoutable(
    OpenTelemetryData data,
    String operationIdAttr,
    Map<String, String> operationIdToOperationKey
  ) {
    if (data.attributes().has(operationIdAttr)) {
      var operationId = data.attributes().get(operationIdAttr).asString();
      if (
        hasText(operationId) &&
        operationIdToOperationKey.containsKey(operationId)
      ) {
        return true;
      }
    }
    // TODO: This should also take request/response into account!
    return (
      data.attributes().has(HTTP_REQUEST_METHOD.getKey()) &&
      data.attributes().has(URL_PATH.getKey())
    );
  }

  private String resolveOperationKey(
    OpenTelemetryData data,
    String operationIdAttr,
    Map<String, String> operationIdToOperationKey
  ) {
    if (data.attributes().has(operationIdAttr)) {
      var operationId = data.attributes().get(operationIdAttr).asString();
      if (hasText(operationId)) {
        var resolvedKey = operationIdToOperationKey.get(operationId);
        if (nonNull(resolvedKey)) {
          return resolvedKey;
        }
      }
    }

    return toOperationKey(
      data.attributes().get(URL_PATH.getKey()).asString(),
      data.attributes().get(HTTP_REQUEST_METHOD.getKey()).asString()
    );
  }

  private record PathItemMapping(
    Function<PathItem, Operation> mappingFunction,
    PathItem.HttpMethod httpMethod
  ) {
    public String httpMethodString() {
      return httpMethod.name();
    }
  }

  private record OpenApiPathIndex(
    Map<String, Operation> operationKeyToOperation,
    Map<String, String> operationIdToOperationKey
  ) {}
}

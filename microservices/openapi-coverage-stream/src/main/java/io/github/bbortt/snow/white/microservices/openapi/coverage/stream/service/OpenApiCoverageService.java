/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
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
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenApiTestContext;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
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

  private final OpenApiCoverageCalculationCoordinator openApiCoverageCalculationCoordinator;

  public Set<OpenApiTestResult> testOpenApi(
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

    var pathToOpenAPIOperationMap = extractPathsAndOperations(openApi);
    var pathToTelemetryMap = groupTelemetryByPath(openTelemetryData);

    return openApiCoverageCalculationCoordinator.calculate(
      pathToOpenAPIOperationMap,
      pathToTelemetryMap
    );
  }

  private Map<String, Operation> extractPathsAndOperations(OpenAPI openApi) {
    Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();

    if (isEmpty(openApi.getPaths())) {
      return pathToOpenAPIOperationMap;
    }

    openApi
      .getPaths()
      .forEach((path, pathItem) -> {
        for (var pathItemMapping : METHOD_ACCESSORS) {
          var operation = pathItemMapping.mappingFunction().apply(pathItem);
          if (nonNull(operation)) {
            pathToOpenAPIOperationMap.put(
              toOperationKey(path, pathItemMapping.httpMethodString()),
              operation
            );
          }
        }
      });

    return pathToOpenAPIOperationMap;
  }

  private Map<String, List<OpenTelemetryData>> groupTelemetryByPath(
    Set<OpenTelemetryData> telemetryData
  ) {
    return telemetryData
      .stream()
      .filter(
        data ->
          // TODO: This should also take request/response into account!
          data.attributes().has(HTTP_REQUEST_METHOD.getKey()) &&
          data.attributes().has(URL_PATH.getKey())
      )
      .collect(
        groupingBy(data -> {
          String method = data
            .attributes()
            .get(HTTP_REQUEST_METHOD.getKey())
            .asString();
          String path = data.attributes().get(URL_PATH.getKey()).asString();
          return toOperationKey(path, method);
        })
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
}

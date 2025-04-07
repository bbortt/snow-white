package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.calculator.OperationKeyCalculator.toOperationKey;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb.AttributeFilter.attributeFilters;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.DELETE;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.GET;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.HEAD;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.OPTIONS;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.PATCH;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.POST;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.PUT;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static org.springframework.util.CollectionUtils.isEmpty;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiCriterionResult;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCoverageService {

  private final OpenApiCoverageCalculationCoordinator openApiCoverageCalculationCoordinator;
  private final OpenTelemetryService openTelemetryService;

  public Set<OpenApiCriterionResult> gatherDataAndCalculateCoverage(
    OpenApiService.OpenApiCoverageRequest openApiCoverageRequest
  ) {
    var openTelemetryData = openTelemetryService.findTracingData(
      openApiCoverageRequest.openApiIdentifier().otelServiceName(),
      openApiCoverageRequest.lookbackWindow(),
      // TODO: Values should be extracted from stream
      attributeFilters().build()
    );

    if (isEmpty(openTelemetryData)) {
      return emptySet();
    }

    return calculateCoverage(
      openApiCoverageRequest.openAPI(),
      openTelemetryData
    );
  }

  private Set<OpenApiCriterionResult> calculateCoverage(
    OpenAPI openApi,
    List<OpenTelemetryData> telemetryData
  ) {
    logger.info(
      "Calculating OpenAPI coverage for {} telemetry data points",
      telemetryData.size()
    );

    var pathToOpenAPIOperationMap = extractPathsAndOperations(openApi);
    var pathToTelemetryMap = groupTelemetryByPath(telemetryData);

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
        if (nonNull(pathItem.getGet())) {
          pathToOpenAPIOperationMap.put(
            toOperationKey(path, GET),
            pathItem.getGet()
          );
        }
        if (nonNull(pathItem.getPost())) {
          pathToOpenAPIOperationMap.put(
            toOperationKey(path, POST),
            pathItem.getPost()
          );
        }
        if (nonNull(pathItem.getPut())) {
          pathToOpenAPIOperationMap.put(
            toOperationKey(path, PUT),
            pathItem.getPut()
          );
        }
        if (nonNull(pathItem.getDelete())) {
          pathToOpenAPIOperationMap.put(
            toOperationKey(path, DELETE),
            pathItem.getDelete()
          );
        }
        if (nonNull(pathItem.getPatch())) {
          pathToOpenAPIOperationMap.put(
            toOperationKey(path, PATCH),
            pathItem.getPatch()
          );
        }
        if (nonNull(pathItem.getHead())) {
          pathToOpenAPIOperationMap.put(
            toOperationKey(path, HEAD),
            pathItem.getHead()
          );
        }
        if (nonNull(pathItem.getOptions())) {
          pathToOpenAPIOperationMap.put(
            toOperationKey(path, OPTIONS),
            pathItem.getOptions()
          );
        }
      });

    return pathToOpenAPIOperationMap;
  }

  private Map<String, List<OpenTelemetryData>> groupTelemetryByPath(
    List<OpenTelemetryData> telemetryData
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
            .asText();
          String path = data.attributes().get(URL_PATH.getKey()).asText();
          return toOperationKey(path, method);
        })
      );
  }
}

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb.AttributeFilter.attributeFilters;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.DELETE;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.GET;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.HEAD;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.OPTIONS;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.PATCH;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.POST;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.HttpAttributes.HttpRequestMethodValues.PUT;
import static java.util.stream.Collectors.groupingBy;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.domain.OpenApiCoverage;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCoverageService {

  private final OpenApiService openApiService;
  private final OpenTelemetryService openTelemetryService;

  public OpenApiCoverage gatherDataAndCalculateCoverage(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    try {
      var openApi = openApiService.findAndParseOpenApi(
        new OpenApiService.OpenApiIdentifier(
          otelServiceName,
          apiName,
          apiVersion
        )
      );

      var openTelemetryData = openTelemetryService.findTracingData(
        "example-application",
        "1d",
        attributeFilters().build()
      );

      return calculateCoverage(openApi, openTelemetryData);
    } catch (OpenApiNotIndexedException | UnparseableOpenApiException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private OpenApiCoverage calculateCoverage(
    OpenAPI openApi,
    List<OpenTelemetryData> telemetryData
  ) {
    logger.info(
      "Calculating OpenAPI coverage for {} telemetry data points",
      telemetryData.size()
    );

    var operationsMap = extractOperations(openApi);
    var pathToTelemetryMap = groupTelemetryByPath(telemetryData);

    return new OpenApiCoverageCalculator(
      operationsMap,
      pathToTelemetryMap
    ).performCalculations();
  }

  private Map<String, Operation> extractOperations(OpenAPI openApi) {
    Map<String, Operation> operationsMap = new HashMap<>();

    if (openApi.getPaths() != null) {
      openApi
        .getPaths()
        .forEach((path, pathItem) -> {
          if (pathItem.getGet() != null) {
            operationsMap.put(getOperationKey(path, GET), pathItem.getGet());
          }
          if (pathItem.getPost() != null) {
            operationsMap.put(getOperationKey(path, POST), pathItem.getPost());
          }
          if (pathItem.getPut() != null) {
            operationsMap.put(getOperationKey(path, PUT), pathItem.getPut());
          }
          if (pathItem.getDelete() != null) {
            operationsMap.put(
              getOperationKey(path, DELETE),
              pathItem.getDelete()
            );
          }
          if (pathItem.getPatch() != null) {
            operationsMap.put(
              getOperationKey(path, PATCH),
              pathItem.getPatch()
            );
          }
          if (pathItem.getHead() != null) {
            operationsMap.put(getOperationKey(path, HEAD), pathItem.getHead());
          }
          if (pathItem.getOptions() != null) {
            operationsMap.put(
              getOperationKey(path, OPTIONS),
              pathItem.getOptions()
            );
          }
        });
    }

    return operationsMap;
  }

  private String getOperationKey(String path, String method) {
    return method + "_" + path;
  }

  private Map<String, List<OpenTelemetryData>> groupTelemetryByPath(
    List<OpenTelemetryData> telemetryData
  ) {
    return telemetryData
      .stream()
      .filter(
        data ->
          data.attributes().has("http.request.method") &&
          data.attributes().has("url.path")
      )
      .collect(
        groupingBy(data -> {
          String method = data.attributes().get("http.request.method").asText();
          String path = data.attributes().get("url.path").asText();
          return getOperationKey(path, method);
        })
      );
  }
}

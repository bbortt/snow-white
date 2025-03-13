package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.AttributeFilter.attributeFilters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCoverageService {

  private final OpenApiService openApiService;
  private final OpenTelemetryService openTelemetryService;

  public void calculateTelemetry(
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
    } catch (OpenApiNotIndexedException | UnparseableOpenApiException e) {
      throw new IllegalArgumentException(e);
    }
  }
}

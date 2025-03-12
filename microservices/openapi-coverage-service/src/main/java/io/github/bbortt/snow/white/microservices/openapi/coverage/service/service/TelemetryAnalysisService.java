package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelemetryAnalysisService {

  private final OpenApiService openApiService;

  public void calculateTelemetry(
    String otelServiceName,
    String apiName,
    String apiVersion
  ) {
    try {
      openApiService.findAndParseOpenApi(
        new OpenApiService.OpenApiIdentifier(
          otelServiceName,
          apiName,
          apiVersion
        )
      );
    } catch (OpenApiNotIndexedException | UnparseableOpenApiException e) {
      throw new IllegalArgumentException(e);
    }
  }
}

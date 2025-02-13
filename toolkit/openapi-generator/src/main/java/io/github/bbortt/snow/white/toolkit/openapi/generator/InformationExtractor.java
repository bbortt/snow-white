package io.github.bbortt.snow.white.toolkit.openapi.generator;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import jakarta.annotation.Nullable;

class InformationExtractor {

  private final String apiNameJsonPath;
  private final String apiVersionJsonPath;
  private final String serviceNameJsonPath;

  InformationExtractor(
    String apiNameJsonPath,
    String apiVersionJsonPath,
    String serviceNameJsonPath
  ) {
    this.apiNameJsonPath = apiNameJsonPath;
    this.apiVersionJsonPath = apiVersionJsonPath;
    this.serviceNameJsonPath = serviceNameJsonPath;
  }

  OpenApiInformation extractFromOpenApi(String openApi) {
    return new OpenApiInformation(
      extractApiName(openApi),
      extractApiVersion(openApi),
      extractServiceName(openApi)
    );
  }

  private @Nullable String extractApiName(String openApi) {
    return extractNullablePath(openApi, apiNameJsonPath);
  }

  private @Nullable String extractApiVersion(String openApi) {
    return extractNullablePath(openApi, apiVersionJsonPath);
  }

  private @Nullable String extractServiceName(String openApi) {
    return extractNullablePath(openApi, serviceNameJsonPath);
  }

  private @Nullable String extractNullablePath(
    String openApi,
    String jsonPath
  ) {
    try {
      return JsonPath.read(openApi, "$." + jsonPath);
    } catch (PathNotFoundException e) {
      return null;
    }
  }
}

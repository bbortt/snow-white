/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.openapi;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InformationExtractor {

  private final String apiNameJsonPath;
  private final String apiVersionJsonPath;
  private final String serviceNameJsonPath;

  public OpenApiInformation extractFromOpenApi(String openApi) {
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

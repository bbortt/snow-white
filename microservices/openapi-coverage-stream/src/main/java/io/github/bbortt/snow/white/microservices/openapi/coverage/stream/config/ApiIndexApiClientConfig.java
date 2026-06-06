/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.client.apiindexapi.ApiClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Configuration
public class ApiIndexApiClientConfig {

  public ApiIndexApiClientConfig(
    ApiClient apiClient,
    OpenApiCoverageStreamProperties openApiCoverageStreamProperties
  ) {
    apiClient.setBasePath(
      openApiCoverageStreamProperties.getApiIndex().getBaseUrl()
    );
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.client.apiindexapi.ApiClient;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.client.apiindexapi.api.ApiIndexApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@EnableRetry
@Configuration
public class ApiIndexApiClientConfig {

  private final RestClient restClient;

  public ApiIndexApiClientConfig(
    @Qualifier("restClient") RestClient restClient
  ) {
    this.restClient = restClient;
  }

  @Bean
  public ApiClient apiClient(
    OpenApiCoverageStreamProperties openApiCoverageStreamProperties
  ) {
    var apiClient = new ApiClient(restClient);
    apiClient.setBasePath(
      openApiCoverageStreamProperties.getApiIndex().getBaseUrl()
    );
    return apiClient;
  }

  @Bean
  public ApiIndexApi apiIndexApi(ApiClient apiClient) {
    return new ApiIndexApi(apiClient);
  }
}

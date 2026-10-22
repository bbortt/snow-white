/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.ApiClient;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.api.ApiIndexApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@EnableRetry
@Configuration
@RequiredArgsConstructor
public class ApiIndexApiClientConfig {

  private final RestClient restClient;

  @Bean
  public ApiClient apiClient(ApiSyncJobProperties apiSyncJobProperties) {
    var apiClient = new ApiClient(restClient);
    apiClient.setBasePath(apiSyncJobProperties.getApiIndex().getBaseUrl());
    return apiClient;
  }

  @Bean
  public ApiIndexApi apiIndexApi(ApiClient apiClient) {
    return new ApiIndexApi(apiClient);
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.api.index.ApiClient;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.api.index.api.ApiIndexApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ApiIndexClientConfig {

  private final String apiIndexUrl;

  public ApiIndexClientConfig(ApiSyncJobProperties apiSyncJobProperties) {
    this.apiIndexUrl = apiSyncJobProperties.getApiIndex().getBaseUrl();
  }

  @Bean
  public ApiIndexApi apiIndexApi(RestClient.Builder builder) {
    var apiIndexClient = new ApiClient(builder.build());
    apiIndexClient.setBasePath(apiIndexUrl);
    return new ApiIndexApi(apiIndexClient);
  }
}

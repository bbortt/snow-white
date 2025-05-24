/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.ApiClient;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BackstageApiConfig {

  private final String backstageUrl;

  public BackstageApiConfig(ApiSyncJobProperties apiSyncJobProperties) {
    this.backstageUrl = apiSyncJobProperties.getBackstage().getBaseUrl();
  }

  @Bean
  public EntityApi backstageEntityApi(RestClient.Builder builder) {
    var apiClient = new ApiClient(builder.build());
    apiClient.setBasePath(backstageUrl);
    return new EntityApi(apiClient);
  }
}

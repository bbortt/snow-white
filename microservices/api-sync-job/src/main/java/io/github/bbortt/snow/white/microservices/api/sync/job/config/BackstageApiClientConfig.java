/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.ApiClient;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackstageApiClientConfig {

  public BackstageApiClientConfig(
    ApiClient apiClient,
    ApiSyncJobProperties apiSyncJobProperties
  ) {
    apiClient.setBasePath(apiSyncJobProperties.getBackstage().getBaseUrl());
  }
}

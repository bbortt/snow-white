/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.ApiClient;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiIndexApiClientConfig {

  public ApiIndexApiClientConfig(
    ApiClient apiClient,
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    apiClient.setBasePath(
      reportCoordinationServiceProperties.getApiIndex().getBaseUrl()
    );
  }
}

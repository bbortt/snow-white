/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.api.index.ApiClient;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiIndexApiClientConfig {

  public ApiIndexApiClientConfig(
    ApiClient apiClient,
    OtelEventFilterStreamProperties otelEventFilterStreamProperties
  ) {
    apiClient.setBasePath(
      otelEventFilterStreamProperties.getApiIndex().getBaseUrl()
    );
  }
}

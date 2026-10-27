/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import io.github.bbortt.snow.white.microservices.mcp.api.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.criteriaapi.api.CriteriaApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.api.ReportApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Wires the generated read clients the MCP tools call through. Each of the three
 * documents generates its own {@code ApiClient}, so each gets its own instance and
 * its own base URL.
 */
@Configuration
@RequiredArgsConstructor
public class SnowWhiteApiClientConfig {

  private final McpApiProperties mcpApiProperties;

  @Bean
  public ApiIndexApi apiIndexApi(RestClient restClient) {
    var apiClient =
      new io.github.bbortt.snow.white.microservices.mcp.api.api.client.apiindexapi.ApiClient(
        restClient
      );
    apiClient.setBasePath(mcpApiProperties.getApiIndex().getBaseUrl());
    return new ApiIndexApi(apiClient);
  }

  @Bean
  public CriteriaApi criteriaApi(RestClient restClient) {
    var apiClient =
      new io.github.bbortt.snow.white.microservices.mcp.api.api.client.criteriaapi.ApiClient(
        restClient
      );
    apiClient.setBasePath(mcpApiProperties.getCriteria().getBaseUrl());
    return new CriteriaApi(apiClient);
  }

  @Bean
  public ReportApi reportApi(RestClient restClient) {
    var apiClient =
      new io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.ApiClient(
        restClient
      );
    apiClient.setBasePath(mcpApiProperties.getReport().getBaseUrl());
    return new ReportApi(apiClient);
  }
}

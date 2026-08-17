/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.ApiClient;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.api.QualityGateApi;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

@EnableRetry
@Configuration
@RequiredArgsConstructor
public class QualityGateApiClientConfig {

  public static final String API_CLIENT_BEAN_NAME = "qualityGateApiRestClient";

  private final RestClient restClient;

  @Bean(API_CLIENT_BEAN_NAME)
  public ApiClient apiClient(
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    var apiClient = new ApiClient(restClient);
    apiClient.setBasePath(
      reportCoordinationServiceProperties.getQualityGateApi().getBaseUrl()
    );
    return apiClient;
  }

  @Bean
  public QualityGateApi qualityGateApi(
    @Qualifier(API_CLIENT_BEAN_NAME) ApiClient apiClient
  ) {
    return new QualityGateApi(apiClient);
  }
}

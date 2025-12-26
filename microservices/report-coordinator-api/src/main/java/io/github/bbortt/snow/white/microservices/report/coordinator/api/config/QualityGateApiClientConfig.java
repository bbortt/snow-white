/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.ApiClient;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QualityGateApiClientConfig {

  public QualityGateApiClientConfig(
    ApiClient apiClient,
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    apiClient.setBasePath(
      reportCoordinationServiceProperties.getQualityGateApiUrl()
    );
  }
}

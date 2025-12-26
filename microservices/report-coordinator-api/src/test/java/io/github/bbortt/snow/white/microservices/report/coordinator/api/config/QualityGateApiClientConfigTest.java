/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.ApiClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class QualityGateApiClientConfigTest {

  @Mock
  private ApiClient apiClientMock;

  @Mock
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  @Nested
  class Constructor {

    @Test
    void shouldConfigureBasePath() {
      var basePath = "basePath";
      doReturn(basePath)
        .when(reportCoordinationServicePropertiesMock)
        .getQualityGateApiUrl();

      new QualityGateApiClientConfig(
        apiClientMock,
        reportCoordinationServicePropertiesMock
      );

      verify(apiClientMock).setBasePath(basePath);
    }
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
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
  class ConstructorTest {

    @Mock
    private ReportCoordinationServiceProperties.QualityGateApiProperties qualityGateApiPropertiesMock;

    @Test
    void shouldConfigureBasePath() {
      doReturn(qualityGateApiPropertiesMock)
        .when(reportCoordinationServicePropertiesMock)
        .getQualityGateApi();

      var basePath = "basePath";
      doReturn(basePath).when(qualityGateApiPropertiesMock).getBaseUrl();

      new QualityGateApiClientConfig(
        apiClientMock,
        reportCoordinationServicePropertiesMock
      );

      verify(apiClientMock).setBasePath(basePath);
    }
  }
}

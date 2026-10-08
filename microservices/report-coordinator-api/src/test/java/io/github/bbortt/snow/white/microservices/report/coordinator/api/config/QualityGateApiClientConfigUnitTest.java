/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.ApiClient;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.api.QualityGateApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class QualityGateApiClientConfigUnitTest {

  @Mock
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  @InjectMocks
  private QualityGateApiClientConfig fixture;

  @Nested
  class ApiClientTest {

    @Mock
    private ReportCoordinationServiceProperties.QualityGateApiProperties qualityGateApiPropertiesMock;

    @Test
    void shouldConfigureBasePath() {
      doReturn(qualityGateApiPropertiesMock)
        .when(reportCoordinationServicePropertiesMock)
        .getQualityGateApi();

      var basePath = "basePath";
      doReturn(basePath).when(qualityGateApiPropertiesMock).getBaseUrl();

      assertThat(fixture.apiClient(reportCoordinationServicePropertiesMock))
        .isInstanceOf(ApiClient.class)
        .extracting(ApiClient::getBasePath)
        .isEqualTo(basePath);
    }
  }

  @Nested
  class QualityGateApiTest {

    @Mock
    private ApiClient apiClientMock;

    @Test
    void isConfiguredWithApiClient() {
      assertThat(fixture.qualityGateApi(apiClientMock))
        .isInstanceOf(QualityGateApi.class)
        .extracting(QualityGateApi::getApiClient)
        .isEqualTo(apiClientMock);
    }
  }
}

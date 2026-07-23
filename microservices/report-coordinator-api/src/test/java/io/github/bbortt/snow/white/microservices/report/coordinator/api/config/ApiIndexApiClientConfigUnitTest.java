/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.ApiClient;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.api.ApiIndexApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiIndexApiClientConfigUnitTest {

  @Mock
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  @InjectMocks
  private ApiIndexApiClientConfig fixture;

  @Nested
  class ApiClientTest {

    @Mock
    private ReportCoordinationServiceProperties.ApiIndexProperties apiIndexPropertiesMock;

    @Test
    void shouldConfigureBasePath() {
      doReturn(apiIndexPropertiesMock)
        .when(reportCoordinationServicePropertiesMock)
        .getApiIndex();

      var basePath = "basePath";
      doReturn(basePath).when(apiIndexPropertiesMock).getBaseUrl();

      assertThat(fixture.apiClient(reportCoordinationServicePropertiesMock))
        .isInstanceOf(ApiClient.class)
        .extracting(ApiClient::getBasePath)
        .isEqualTo(basePath);
    }
  }

  @Nested
  class ApiIndexApiTest {

    @Mock
    private ApiClient apiClientMock;

    @Test
    void isConfiguredWithApiClient() {
      assertThat(fixture.apiIndexApi(apiClientMock))
        .isInstanceOf(ApiIndexApi.class)
        .extracting(ApiIndexApi::getApiClient)
        .isEqualTo(apiClientMock);
    }
  }
}

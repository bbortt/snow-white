/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.ApiClient;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.api.ApiIndexApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith({ MockitoExtension.class })
class ApiIndexApiClientConfigUnitTest {

  @Mock
  private RestClient restClientMock;

  @Mock
  private ApiSyncJobProperties apiSyncJobPropertiesMock;

  @InjectMocks
  private ApiIndexApiClientConfig fixture;

  @Nested
  class ApiClientTest {

    @Mock
    private ApiSyncJobProperties.ApiIndexProperties apiIndexPropertiesMock;

    @Test
    void shouldConfigureBasePath() {
      doReturn(apiIndexPropertiesMock)
        .when(apiSyncJobPropertiesMock)
        .getApiIndex();

      var basePath = "basePath";
      doReturn(basePath).when(apiIndexPropertiesMock).getBaseUrl();

      assertThat(fixture.apiClient(apiSyncJobPropertiesMock))
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

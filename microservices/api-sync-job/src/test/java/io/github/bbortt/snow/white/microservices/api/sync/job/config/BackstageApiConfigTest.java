/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.ApiClient;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.api.EntityApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith({ MockitoExtension.class })
class BackstageApiConfigTest {

  private static final String BASE_URL = "http://localhost:8080";

  @Mock
  private ApiSyncJobProperties apiSyncJobProperties;

  @Mock
  private ApiSyncJobProperties.BackstageProperties backstagePropertiesMock;

  private BackstageApiConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    doReturn(backstagePropertiesMock).when(apiSyncJobProperties).getBackstage();
    doReturn(BASE_URL).when(backstagePropertiesMock).getBaseUrl();

    fixture = new BackstageApiConfig(apiSyncJobProperties);
  }

  @Nested
  class Constructor {

    @Test
    void shouldExtractQualityGateApiUrl() {
      assertThat(fixture).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class BackstageEntityApi {

    @Mock
    private RestClient.Builder builderMock;

    @Test
    void shouldBeBuilt() {
      assertThat(fixture.backstageEntityApi(builderMock))
        .isNotNull()
        .extracting(EntityApi::getApiClient)
        .extracting(ApiClient::getBasePath)
        .isEqualTo(BASE_URL);
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.backstage.ApiClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class BackstageApiClientConfigTest {

  @Mock
  private ApiClient apiClientMock;

  @Mock
  private ApiSyncJobProperties apiSyncJobPropertiesMock;

  @Nested
  class Constructor {

    @Mock
    private ApiSyncJobProperties.BackstageProperties backstagePropertiesMock;

    @Test
    void shouldConfigureBasePath() {
      doReturn(backstagePropertiesMock)
        .when(apiSyncJobPropertiesMock)
        .getBackstage();

      var basePath = "basePath";
      doReturn(basePath).when(backstagePropertiesMock).getBaseUrl();

      new BackstageApiClientConfig(apiClientMock, apiSyncJobPropertiesMock);

      verify(apiClientMock).setBasePath(basePath);
    }
  }
}

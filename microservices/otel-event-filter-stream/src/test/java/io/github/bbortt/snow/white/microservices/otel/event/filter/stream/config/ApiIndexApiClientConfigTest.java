/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.api.index.ApiClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiIndexApiClientConfigTest {

  @Mock
  private ApiClient apiClientMock;

  @Mock
  private OtelEventFilterStreamProperties otelEventFilterStreamProperties;

  @Nested
  class Constructor {

    @Mock
    private OtelEventFilterStreamProperties.ApiIndexProperties apiIndexPropertiesMock;

    @Test
    void shouldConfigureBasePath() {
      doReturn(apiIndexPropertiesMock)
        .when(otelEventFilterStreamProperties)
        .getApiIndex();

      var basePath = "basePath";
      doReturn(basePath).when(apiIndexPropertiesMock).getBaseUrl();

      new ApiIndexApiClientConfig(
        apiClientMock,
        otelEventFilterStreamProperties
      );

      verify(apiClientMock).setBasePath(basePath);
    }
  }
}

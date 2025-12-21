/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.api.index.ApiClient;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.api.index.api.ApiIndexApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith({ MockitoExtension.class })
class ApiIndexClientConfigTest {

  private static final String BASE_URL = "http://localhost:8080";

  @Mock
  private OtelEventFilterStreamProperties otelEventFilterStreamPropertiesMock;

  @Mock
  private OtelEventFilterStreamProperties.ApiIndexProperties apiIndexPropertiesMock;

  private ApiIndexClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    doReturn(apiIndexPropertiesMock)
      .when(otelEventFilterStreamPropertiesMock)
      .getApiIndex();
    doReturn(BASE_URL).when(apiIndexPropertiesMock).getBaseUrl();

    fixture = new ApiIndexClientConfig(otelEventFilterStreamPropertiesMock);
  }

  @Nested
  class Constructor {

    @Test
    void shouldExtractApiIndexUrl() {
      assertThat(fixture).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class ApiIndexApiTest {

    @Mock
    private RestClient.Builder builderMock;

    @Test
    void shouldBeBuilt() {
      assertThat(fixture.apiIndexApi(builderMock))
        .isNotNull()
        .extracting(ApiIndexApi::getApiClient)
        .extracting(ApiClient::getBasePath)
        .isEqualTo(BASE_URL);
    }
  }
}

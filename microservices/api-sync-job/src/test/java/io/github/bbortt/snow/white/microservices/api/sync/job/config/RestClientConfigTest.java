/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class RestClientConfigTest {

  private RestClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new RestClientConfig();
  }

  @Nested
  class RestClientBuilder {

    @Mock
    private JsonMapper jsonMapperMock;

    @Test
    void shouldBeConfiguredWithJsonMapperBean() {
      RestClient.Builder restClientBuilder = fixture.restClientBuilder(
        jsonMapperMock
      );
      assertThat(restClientBuilder)
        .extracting("messageConverters")
        .isNotNull()
        .asInstanceOf(LIST)
        .satisfiesExactly(messageConverter ->
          assertThat(messageConverter)
            .asInstanceOf(type(JacksonJsonHttpMessageConverter.class))
            .satisfies(converter ->
              assertThat(converter.getMapper()).isEqualTo(jsonMapperMock)
            )
        );
    }

    @Test
    void shouldAcceptOctetStream() {
      RestClient.Builder restClientBuilder = fixture.restClientBuilder(
        jsonMapperMock
      );
      assertThat(restClientBuilder)
        .extracting("messageConverters")
        .isNotNull()
        .asInstanceOf(LIST)
        .satisfiesExactly(messageConverter ->
          assertThat(messageConverter)
            .asInstanceOf(type(JacksonJsonHttpMessageConverter.class))
            .satisfies(converter ->
              assertThat(converter.getSupportedMediaTypes()).containsExactly(
                APPLICATION_JSON,
                APPLICATION_OCTET_STREAM
              )
            )
        );
    }
  }
}

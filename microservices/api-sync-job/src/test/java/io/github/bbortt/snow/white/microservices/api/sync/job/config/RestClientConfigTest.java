/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.HttpMessageConverters;
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
  class RestClientTest {

    @Mock
    private RestClient.Builder restClientBuilderMock;

    @Test
    void createsRestClient() {
      var restClientMock = mock(RestClient.class);
      doReturn(restClientMock).when(restClientBuilderMock).build();

      var result = fixture.restClient(restClientBuilderMock);

      assertThat(result).isEqualTo(restClientMock);
    }
  }

  @Nested
  class RestClientBuilderTest {

    @Mock
    private JsonMapper jsonMapperMock;

    @Mock
    private HttpMessageConverters.ClientBuilder clientBuilderMock;

    @Test
    void shouldBeConfiguredWithJsonMapperBean() {
      RestClient.Builder restClientBuilder = fixture.restClientBuilder(
        jsonMapperMock
      );

      assertThat(restClientBuilder)
        .extracting("convertersConfigurer")
        .isNotNull()
        .asInstanceOf(type(Consumer.class))
        .matches(
          messageConverter -> {
            messageConverter.accept(clientBuilderMock);

            ArgumentCaptor<
              JacksonJsonHttpMessageConverter
            > jacksonJsonHttpMessageConverterArgumentCaptor = captor();
            verify(clientBuilderMock).addCustomConverter(
              jacksonJsonHttpMessageConverterArgumentCaptor.capture()
            );

            assertThat(jacksonJsonHttpMessageConverterArgumentCaptor.getValue())
              .isNotNull()
              .satisfies(converter ->
                assertThat(converter.getMapper()).isEqualTo(jsonMapperMock)
              );

            return true;
          },
          "JacksonJsonHttpMessageConverter should have been configured with JsonMapper"
        );
    }

    @Test
    void shouldAcceptOctetStream() {
      RestClient.Builder restClientBuilder = fixture.restClientBuilder(
        jsonMapperMock
      );

      assertThat(restClientBuilder)
        .extracting("convertersConfigurer")
        .isNotNull()
        .asInstanceOf(type(Consumer.class))
        .matches(
          messageConverter -> {
            messageConverter.accept(clientBuilderMock);

            ArgumentCaptor<
              JacksonJsonHttpMessageConverter
            > jacksonJsonHttpMessageConverterArgumentCaptor = captor();
            verify(clientBuilderMock).addCustomConverter(
              jacksonJsonHttpMessageConverterArgumentCaptor.capture()
            );

            assertThat(jacksonJsonHttpMessageConverterArgumentCaptor.getValue())
              .isNotNull()
              .satisfies(converter ->
                assertThat(converter.getSupportedMediaTypes()).containsExactly(
                  APPLICATION_JSON,
                  APPLICATION_OCTET_STREAM
                )
              );

            return true;
          },
          "JacksonJsonHttpMessageConverter should have been configured to accept both media types 'application/json' and 'application/octet-stream'"
        );
    }
  }
}

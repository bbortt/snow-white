/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.jackson.ApiInformationDeserializer;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.type.ClassKey;

@ExtendWith({ MockitoExtension.class })
class RestClientConfigTest {

  @Mock
  private ApiInformationDeserializer apiInformationDeserializerMock;

  private RestClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new RestClientConfig(apiInformationDeserializerMock);
  }

  @Nested
  class RestClientBuilder {

    @Mock
    private JsonFactory jsonFactoryMock;

    @Mock
    private JsonMapper jsonMapperMock;

    @Test
    void shouldBeConfiguredWithCustomDeserializer() {
      doReturn(jsonFactoryMock).when(jsonMapperMock).tokenStreamFactory();

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
            .satisfies(
              converter ->
                assertThat(converter.getMapper())
                  .isNotSameAs(jsonMapperMock)
                  .extracting(jsonMapper ->
                    jsonMapper.getRegisteredModules().toList()
                  )
                  .asInstanceOf(LIST)
                  .satisfiesExactly(registeredModule ->
                    assertThat(registeredModule)
                      .extracting("_deserializers")
                      .asInstanceOf(type(SimpleDeserializers.class))
                      .extracting("_classMappings")
                      .asInstanceOf(
                        map(ClassKey.class, ValueDeserializer.class)
                      )
                      .hasSize(1)
                      .hasEntrySatisfying(
                        new Condition<>(
                          classKey ->
                            classKey.equals(new ClassKey(ApiInformation.class)),
                          "Key is ApiInformation.class"
                        ),
                        new Condition<>(
                          value -> value.equals(apiInformationDeserializerMock),
                          "Value is apiInformationDeserializerMock"
                        )
                      )
                  ),
              converter ->
                assertThat(converter.getSupportedMediaTypes()).containsExactly(
                  APPLICATION_JSON,
                  APPLICATION_OCTET_STREAM
                )
            )
        );
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.jackson.ApiInformationDeserializer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

  private final ApiInformationDeserializer apiInformationDeserializer;

  @Bean
  @Scope("prototype")
  public RestClient.Builder restClientBuilder(JsonMapper jsonMapper) {
    var customizedJsonMapper = new JsonMapper.Builder(
      jsonMapper.tokenStreamFactory()
    )
      .addModule(
        new SimpleModule().addDeserializer(
          ApiInformation.class,
          apiInformationDeserializer
        )
      )
      .build();

    var jacksonJsonHttpMessageConverter = new JacksonJsonHttpMessageConverter(
      customizedJsonMapper
    );
    jacksonJsonHttpMessageConverter.setSupportedMediaTypes(
      List.of(APPLICATION_JSON, APPLICATION_OCTET_STREAM)
    );

    return RestClient.builder().messageConverters(
      singletonList(jacksonJsonHttpMessageConverter)
    );
  }
}

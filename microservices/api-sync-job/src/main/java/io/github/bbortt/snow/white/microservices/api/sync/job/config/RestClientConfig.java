/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class RestClientConfig {

  @Bean
  @Scope("prototype")
  public RestClient restClient(RestClient.Builder restClientBuilder) {
    return restClientBuilder.build();
  }

  @Bean
  @Scope("prototype")
  public RestClient.Builder restClientBuilder(JsonMapper jsonMapper) {
    var jacksonJsonHttpMessageConverter = new JacksonJsonHttpMessageConverter(
      jsonMapper
    );
    jacksonJsonHttpMessageConverter.setSupportedMediaTypes(
      List.of(APPLICATION_JSON, APPLICATION_OCTET_STREAM)
    );

    return RestClient.builder().configureMessageConverters(clientBuilder ->
      clientBuilder.addCustomConverter(jacksonJsonHttpMessageConverter)
    );
  }
}

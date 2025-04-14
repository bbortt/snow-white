/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class MessageConverterConfig {

  public MessageConverterConfig(
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter
  ) {
    mappingJackson2HttpMessageConverter.setSupportedMediaTypes(
      List.of(APPLICATION_JSON, APPLICATION_OCTET_STREAM)
    );
  }
}

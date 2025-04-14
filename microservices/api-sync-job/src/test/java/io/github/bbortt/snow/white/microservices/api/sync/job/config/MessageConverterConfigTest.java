/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@ExtendWith({ MockitoExtension.class })
class MessageConverterConfigTest {

  @Mock
  private MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverterMock;

  @Nested
  class Constructor {

    @Test
    void setsSupportedMediaTypes() {
      new MessageConverterConfig(mappingJackson2HttpMessageConverterMock);

      verify(mappingJackson2HttpMessageConverterMock).setSupportedMediaTypes(
        List.of(APPLICATION_JSON, APPLICATION_OCTET_STREAM)
      );
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;

@IntegrationTest
@TestPropertySource(
  properties = {
    "snow.white.otel.event.filter.api-index.base-url=http://localhost:8085",
  }
)
class RestClientConfigIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Nested
  class RestClientBuilder {

    @Test
    void isPrototypeBean() {
      assertThat(
        applicationContext.getBean(RestClient.Builder.class)
      ).isNotSameAs(applicationContext.getBean(RestClient.Builder.class));
    }
  }
}

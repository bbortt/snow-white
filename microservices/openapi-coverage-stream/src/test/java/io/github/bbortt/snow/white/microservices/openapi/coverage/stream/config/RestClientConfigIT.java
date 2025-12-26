/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;

public class RestClientConfigIT extends AbstractOpenApiCoverageServiceIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Nested
  class RestClientTest {

    @Test
    void isPrototypeBean() {
      assertThat(applicationContext.getBean(RestClient.class))
        .isInstanceOf(RestClient.class)
        .isNotSameAs(applicationContext.getBean(RestClient.class));
    }
  }
}

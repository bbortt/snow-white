/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestClient;

@IntegrationTest
class RestClientConfigIT {

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

  @Nested
  class RestClientBuilderTest {

    @Test
    void isPrototypeBean() {
      assertThat(applicationContext.getBean(RestClient.Builder.class))
        .isInstanceOf(RestClient.Builder.class)
        .isNotSameAs(applicationContext.getBean(RestClient.Builder.class));
    }
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.ApiClient;
import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.api.ApiIndexApi;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@IntegrationTest
class ApiIndexApiClientConfigIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Nested
  class ApiClientTest {

    @Test
    void isBean() {
      assertThat(applicationContext.getBean(ApiClient.class)).isInstanceOf(
        ApiClient.class
      );
    }
  }

  @Nested
  class ApiIndexApiTest {

    @Test
    void isBean() {
      assertThat(applicationContext.getBean(ApiIndexApi.class)).isInstanceOf(
        ApiIndexApi.class
      );
    }
  }
}

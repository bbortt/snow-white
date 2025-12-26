/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientConfigTest {

  private RestClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new RestClientConfig();
  }

  @Nested
  class RestClientBuilder {

    @Test
    void createsRestClientBuilder() {
      assertThat(fixture.restClient()).isInstanceOf(RestClient.class);
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith({ MockitoExtension.class })
class RestClientConfigTest {

  private RestClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new RestClientConfig();
  }

  @Nested
  class RestClientTest {

    @Test
    void createsRestClient() {
      var result = fixture.restClient();

      assertThat(result).isInstanceOf(RestClient.class);
    }
  }
}

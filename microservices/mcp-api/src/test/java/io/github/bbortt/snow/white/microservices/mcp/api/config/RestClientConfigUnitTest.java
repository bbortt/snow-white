/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RestClientConfigUnitTest {

  /** Prototype-scoped, so the three generated clients cannot share one instance. */
  @Test
  void createsANewRestClientPerInjectionPoint() {
    var fixture = new RestClientConfig();

    assertThat(fixture.restClient())
      .isInstanceOf(RestClient.class)
      .isNotSameAs(fixture.restClient());
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.handler;

import static io.github.bbortt.snow.white.microservices.api.gateway.handler.GatewayErrorHandler.DOWNSTREAM_UNAVAILABLE_HEADER;
import static io.github.bbortt.snow.white.microservices.api.gateway.handler.GatewayErrorHandler.ERROR_DETAILS_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import io.github.bbortt.snow.white.microservices.api.gateway.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@IntegrationTest
@AutoConfigureWebTestClient
class GatewayErrorHandlerIT {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void shouldReturnServiceUnavailableStatusCodeForUnreachableHost() {
    webTestClient
      .get()
      .uri("/api/rest/v1/reports")
      .exchange()
      .expectStatus()
      .isEqualTo(SERVICE_UNAVAILABLE)
      .expectHeader()
      .value(ERROR_DETAILS_HEADER_NAME, value -> assertThat(value).isEqualTo(DOWNSTREAM_UNAVAILABLE_HEADER));
  }
}

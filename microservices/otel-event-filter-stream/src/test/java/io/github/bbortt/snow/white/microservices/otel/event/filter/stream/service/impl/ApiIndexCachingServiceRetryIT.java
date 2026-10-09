/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.EnableWireMock;

@EnableWireMock
@IntegrationTest
@TestPropertySource(
  properties = {
    "snow.white.otel.event.filter.api-index.base-url=${wiremock.server.baseUrl}",
  }
)
class ApiIndexCachingServiceRetryIT {

  private static final String SERVICE_NAME = "retry-service";
  private static final String API_NAME = "retry-api";
  private static final String API_VERSION = "1.0.0";

  private static final String EXISTS_PATH =
    "/api/rest/v1/apis/" +
    SERVICE_NAME +
    "/" +
    API_NAME +
    "/" +
    API_VERSION +
    "/exists";

  @Autowired
  private ApiIndexCachingService apiIndexCachingService;

  @BeforeEach
  void setUp() {
    reset();
  }

  @Test
  void shouldRetryAndRecoverApiExists() {
    stubFor(get(urlPathEqualTo(EXISTS_PATH)).willReturn(serverError()));

    assertThat(
      apiIndexCachingService.apiExists(SERVICE_NAME, API_NAME, API_VERSION)
    ).isFalse();

    verify(3, getRequestedFor(urlPathEqualTo(EXISTS_PATH)));
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.wiremock.spring.EnableWireMock;

@EnableWireMock
@SpringBootTest(classes = OpenApiSourceClientRetryIT.TestConfig.class)
class OpenApiSourceClientRetryIT {

  private static final String SPEC_PATH = "/openapi/swagger.yaml";

  @EnableRetry
  @Import(OpenApiSourceClient.class)
  static class TestConfig {}

  @Autowired
  private OpenApiSourceClient fixture;

  @Value("${wiremock.server.baseUrl}")
  private String wireMockBaseUrl;

  @BeforeEach
  void beforeEachSetup() {
    reset();
  }

  @Nested
  class FetchContentTest {

    @Test
    void shouldReturnContent_whenReachable() throws IOException {
      stubFor(get(urlEqualTo(SPEC_PATH)).willReturn(ok("openapi: 3.0.0")));

      var content = fixture.fetchContent(wireMockBaseUrl + SPEC_PATH);

      assertThat(content).isEqualTo("openapi: 3.0.0");
    }

    @Test
    void shouldRetryBeforePropagatingFailure() {
      stubFor(get(urlEqualTo(SPEC_PATH)).willReturn(serverError()));

      assertThatThrownBy(() ->
        fixture.fetchContent(wireMockBaseUrl + SPEC_PATH)
      ).isInstanceOf(IOException.class);

      verify(3, getRequestedFor(urlEqualTo(SPEC_PATH)));
    }
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.wiremock.spring.EnableWireMock;

@EnableWireMock
@SpringBootTest(classes = TempoQueryClientRetryIT.TestConfig.class)
class TempoQueryClientRetryIT {

  private static final String TRACE_ID = "f2c79a8d4bce407aa65c1e7289f6febb";

  @EnableRetry
  static class TestConfig {

    @Bean
    RestClient tempoRestClient(
      @Value("${wiremock.server.baseUrl}") String wireMockBaseUrl
    ) {
      return RestClient.builder().baseUrl(wireMockBaseUrl).build();
    }

    @Bean
    TempoQueryClient tempoQueryClient() {
      return new TempoQueryClient();
    }
  }

  @Autowired
  private TempoQueryClient fixture;

  @BeforeEach
  void beforeEachSetup() {
    reset();
  }

  @Nested
  class SearchTest {

    @Test
    void shouldRetryBeforePropagatingFailure() {
      stubFor(get(urlPathEqualTo("/api/search")).willReturn(serverError()));

      assertThatThrownBy(() ->
        fixture.search("{ resource.service.name = \"svc\" }", 0L, 1L)
      ).isInstanceOf(HttpServerErrorException.class);

      verify(3, getRequestedFor(urlPathEqualTo("/api/search")));
    }
  }

  @Nested
  class GetTraceByIdTest {

    @Test
    void shouldRetryBeforePropagatingFailure() {
      stubFor(
        get(urlEqualTo("/api/v2/traces/" + TRACE_ID)).willReturn(serverError())
      );

      assertThatThrownBy(() -> fixture.getTraceById(TRACE_ID)).isInstanceOf(
        HttpServerErrorException.class
      );

      verify(3, getRequestedFor(urlEqualTo("/api/v2/traces/" + TRACE_ID)));
    }
  }
}

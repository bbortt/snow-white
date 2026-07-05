/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;
import org.wiremock.spring.EnableWireMock;

@EnableWireMock
@SpringBootTest(
  classes = TempoRestClientConfigIT.TestConfig.class,
  properties = {
    "tempo.url=${wiremock.server.baseUrl}",
    "tempo.token=my-token",
  }
)
class TempoRestClientConfigIT {

  @EnableConfigurationProperties(TempoProperties.class)
  @Import(TempoRestClientConfig.class)
  static class TestConfig {}

  @Autowired
  @Qualifier("tempoRestClient")
  private RestClient tempoRestClient;

  @Nested
  class TempoRestClientTest {

    @Test
    void shouldSendBearerAuthorizationHeader() {
      assertThat(tempoRestClient).isNotNull();

      stubFor(get(urlEqualTo("/api/search")).willReturn(ok()));

      tempoRestClient.get().uri("/api/search").retrieve().toBodilessEntity();

      verify(
        getRequestedFor(urlEqualTo("/api/search")).withHeader(
          AUTHORIZATION,
          equalTo("Bearer my-token")
        )
      );
    }
  }
}

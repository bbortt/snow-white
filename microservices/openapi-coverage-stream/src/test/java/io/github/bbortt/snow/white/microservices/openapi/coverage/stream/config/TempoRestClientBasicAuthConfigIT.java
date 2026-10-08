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

import java.util.Base64;
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
  classes = TempoRestClientBasicAuthConfigIT.TestConfig.class,
  properties = {
    "tempo.url=${wiremock.server.baseUrl}",
    "tempo.username=snow-white",
    "tempo.password=s3cr3t",
  }
)
class TempoRestClientBasicAuthConfigIT {

  @Autowired
  @Qualifier("tempoRestClient")
  private RestClient tempoRestClient;

  @Nested
  class TempoRestClientTest {

    @Test
    void shouldSendBasicAuthorizationHeader() {
      assertThat(tempoRestClient).isNotNull();

      stubFor(get(urlEqualTo("/api/search")).willReturn(ok()));

      tempoRestClient.get().uri("/api/search").retrieve().toBodilessEntity();

      var expectedCredentials = Base64.getEncoder().encodeToString(
        "snow-white:s3cr3t".getBytes()
      );

      verify(
        getRequestedFor(urlEqualTo("/api/search")).withHeader(
          AUTHORIZATION,
          equalTo("Basic " + expectedCredentials)
        )
      );
    }
  }

  @Import(TempoRestClientConfig.class)
  @EnableConfigurationProperties(TempoProperties.class)
  static class TestConfig {}
}

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
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import com.github.tomakehurst.wiremock.WireMockServer;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TempoRestClientConfigUnitTest {

  private final TempoRestClientConfig fixture = new TempoRestClientConfig();

  private WireMockServer wireMockServer;
  private TempoProperties tempoProperties;

  @BeforeEach
  void beforeEachSetup() {
    wireMockServer = new WireMockServer(0);
    wireMockServer.start();

    tempoProperties = new TempoProperties();
    tempoProperties.setUrl(wireMockServer.baseUrl());

    wireMockServer.stubFor(get(urlEqualTo("/api/search")).willReturn(ok()));
  }

  @AfterEach
  void afterEachTeardown() {
    wireMockServer.stop();
  }

  @Nested
  class TempoRestClientTest {

    @Test
    void shouldSendBearerAuthorizationHeader_whenTokenConfigured() {
      tempoProperties.setToken("my-token");

      fixture
        .tempoRestClient(tempoProperties)
        .get()
        .uri("/api/search")
        .retrieve()
        .toBodilessEntity();

      wireMockServer.verify(
        getRequestedFor(urlEqualTo("/api/search")).withHeader(
          AUTHORIZATION,
          equalTo("Bearer my-token")
        )
      );
    }

    @Test
    void shouldSendBasicAuthorizationHeader_whenUsernameAndPasswordConfigured() {
      tempoProperties.setUsername("user");
      tempoProperties.setPassword("pass");

      fixture
        .tempoRestClient(tempoProperties)
        .get()
        .uri("/api/search")
        .retrieve()
        .toBodilessEntity();

      var expectedCredentials = Base64.getEncoder().encodeToString(
        "user:pass".getBytes()
      );

      wireMockServer.verify(
        getRequestedFor(urlEqualTo("/api/search")).withHeader(
          AUTHORIZATION,
          equalTo("Basic " + expectedCredentials)
        )
      );
    }
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.when;
import static java.lang.Integer.parseInt;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.github.bbortt.snow.white.microservices.api.gateway.Main;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.wiremock.spring.EnableWireMock;

@Isolated
@DirtiesContext
@EnableWireMock
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.gateway.api-index-api-url=http://localhost:8085",
    "snow.white.api.gateway.mcp-api-url=${wiremock.server.baseUrl}",
    "snow.white.api.gateway.quality-gate-api-url=http://localhost:8081",
    "snow.white.api.gateway.report-coordinator-api-url=http://localhost:8084",
  },
  webEnvironment = RANDOM_PORT
)
class McpApiIT {

  @Value("${local.server.port}")
  String localServerPort;

  @BeforeEach
  void beforeEachSetup() {
    RestAssured.port = parseInt(localServerPort);

    reset();
  }

  @Test
  void mcpEndpointRequestShouldBeForwarded() {
    stubFor(post("/mcp").willReturn(ok()));

    when().post("/mcp").then().statusCode(200);

    verify(postRequestedFor(urlEqualTo("/mcp")));
  }

  /**
   * The transport appends a session path of its own, so the prefix has to forward unrewritten.
   */
  @Test
  void mcpSubPathRequestShouldBeForwarded() {
    stubFor(post("/mcp/message").willReturn(ok()));

    when().post("/mcp/message").then().statusCode(200);

    verify(postRequestedFor(urlEqualTo("/mcp/message")));
  }
}

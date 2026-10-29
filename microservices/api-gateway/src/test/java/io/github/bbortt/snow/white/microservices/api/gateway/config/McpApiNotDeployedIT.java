/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static io.restassured.RestAssured.when;
import static java.lang.Integer.parseInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.annotation.VerifiesNf;
import io.github.bbortt.snow.white.microservices.api.gateway.Main;
import io.restassured.RestAssured;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

/**
 * The gateway without an MCP backend: the three REST URLs are configured, {@code mcp-api-url} is
 * not.
 */
@Isolated
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.gateway.api-index-api-url=http://localhost:8085",
    "snow.white.api.gateway.quality-gate-api-url=http://localhost:8081",
    "snow.white.api.gateway.report-coordinator-api-url=http://localhost:8084",
  },
  webEnvironment = RANDOM_PORT
)
class McpApiNotDeployedIT {

  @Value("${local.server.port}")
  String localServerPort;

  @Autowired
  private RouteLocator snowWhiteRouteLocator;

  @BeforeEach
  void beforeEachSetup() {
    RestAssured.port = parseInt(localServerPort);
  }

  @Test
  @VerifiesNf(NfTraceables.NF_011_MCP_SERVER_IS_AN_OPT_IN_DEPLOYMENT)
  void configuresNoMcpRoute() {
    var routes = snowWhiteRouteLocator.getRoutes().collectList().block(Duration.ofSeconds(5));

    assertThat(routes)
      .hasSize(6)
      .noneSatisfy(route -> assertThat(route.getId()).isEqualTo("mcp-api"));
  }

  @Test
  @VerifiesNf(NfTraceables.NF_011_MCP_SERVER_IS_AN_OPT_IN_DEPLOYMENT)
  void mcpEndpointRequestIsNotFound() {
    when().post("/mcp").then().statusCode(404);

    when().post("/mcp/message").then().statusCode(404);
  }
}

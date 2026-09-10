/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
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
    "snow.white.api.gateway.api-index-api-url=${wiremock.server.baseUrl}",
    "snow.white.api.gateway.quality-gate-api-url=http://localhost:8081",
    "snow.white.api.gateway.report-coordinator-api-url=http://localhost:8084",
  },
  webEnvironment = RANDOM_PORT
)
class ApiIndexApiIT {

  @Value("${local.server.port}")
  String localServerPort;

  @BeforeEach
  void beforeEachSetup() {
    RestAssured.port = parseInt(localServerPort);

    reset();
  }

  @Test
  void apisResourceRequestShouldBeForwarded() {
    stubFor(get("/api/rest/v1/apis").willReturn(ok()));

    when().get("/api/rest/v1/apis").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/apis")));
  }

  @Test
  void apiByPathResourceRequestShouldBeForwarded() {
    var service = "my-service";
    var api = "my-api";
    var version = "1.0.0";

    stubFor(
      get(urlPathTemplate("/api/rest/v1/apis/{service}/{api}/{version}"))
        .withPathParam("service", equalTo(service))
        .withPathParam("api", equalTo(api))
        .withPathParam("version", equalTo(version))
        .willReturn(ok())
    );

    when().get("/api/rest/v1/apis/{service}/{api}/{version}", service, api, version).then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/apis/" + service + "/" + api + "/" + version)));
  }

  @Test
  void shouldTransformSwaggerApiRequest() {
    stubFor(get("/v3/api-docs").willReturn(ok()));

    when().get("/v3/api-docs/api-index-api").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/v3/api-docs")));
  }
}

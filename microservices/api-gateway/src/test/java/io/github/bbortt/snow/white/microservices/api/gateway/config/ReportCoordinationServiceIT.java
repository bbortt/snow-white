/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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
    "snow.white.api.gateway.quality-gate-api-url=http://localhost:8081",
    "snow.white.api.gateway.report-coordinator-api-url=${wiremock.server.baseUrl}",
  },
  webEnvironment = RANDOM_PORT
)
class ReportCoordinationServiceIT {

  @Value("${local.server.port}")
  String localServerPort;

  @BeforeEach
  void beforeEachSetup() {
    RestAssured.port = parseInt(localServerPort);
  }

  @Test
  void qualityGateCalculationRequestShouldBeForwarded() {
    var qualityGateConfigName = "report-coordinator-api";

    stubFor(
      post(urlPathTemplate("/api/rest/v1/quality-gates/{qualityGateConfigName}/calculate"))
        .withPathParam("qualityGateConfigName", equalTo(qualityGateConfigName))
        .willReturn(ok())
    );

    when().post("/api/rest/v1/quality-gates/{qualityGateConfigName}/calculate", qualityGateConfigName).then().statusCode(200);

    verify(postRequestedFor(urlEqualTo("/api/rest/v1/quality-gates/" + qualityGateConfigName + "/calculate")));
  }

  @Test
  void reportsResourceRequestShouldBeForwarded() {
    stubFor(get("/api/rest/v1/reports").willReturn(ok()));

    when().get("/api/rest/v1/reports").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/reports")));
  }

  @Test
  void reportByIdResourceRequestShouldBeForwarded() {
    var reportId = "report-id";

    stubFor(get(urlPathTemplate("/api/rest/v1/reports/{reportId}")).withPathParam("reportId", equalTo(reportId)).willReturn(ok()));

    when().get("/api/rest/v1/reports/{reportId}", reportId).then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/reports/" + reportId)));
  }

  @Test
  void shouldTransformSwaggerApiRequest() {
    stubFor(get("/v3/api-docs").willReturn(ok()));

    when().get("/v3/api-docs/report-coordinator-api").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/v3/api-docs")));
  }
}

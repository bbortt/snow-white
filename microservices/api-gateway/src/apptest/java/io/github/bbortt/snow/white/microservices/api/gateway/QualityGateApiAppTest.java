/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.RestAssured.when;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;

class QualityGateApiAppTest {

  @BeforeAll
  static void beforeAllSetup() {
    RestAssured.baseURI = format("http://%s", getProperty("api-gateway.host","localhost"));
    RestAssured.port = parseInt(getProperty("api-gateway.port","8080"));

    WireMock.configureFor(
      getProperty("wiremock.host","localhost"),
      parseInt(getProperty("wiremock.port","9000"))
    );
  }

  @Test
  void openApiCriteriaResourceShouldBeForwarded() {
    stubFor(get("/api/rest/v1/criteria/openapi").willReturn(ok()));

    when().get("/api/rest/v1/criteria/openapi").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/criteria/openapi")));
  }

  @Test
  void qualityGatesResourceRequestShouldBeForwarded() {
    stubFor(get("/api/rest/v1/quality-gates").willReturn(ok()));

    when().get("/api/rest/v1/quality-gates").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/quality-gates")));
  }

  @Test
  void qualityGateByIdResourceRequestShouldBeForwarded() {
    var qualityGateConfigName = "report-id";

    stubFor(
      get(urlPathTemplate("/api/rest/v1/quality-gates/{qualityGateConfigName}"))
        .withPathParam("qualityGateConfigName", equalTo(qualityGateConfigName))
        .willReturn(ok())
    );

    when().get("/api/rest/v1/quality-gates/{reportId}", qualityGateConfigName).then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/quality-gates/" + qualityGateConfigName)));
  }

  @Test
  void shouldTransformSwaggerApiRequest() {
    stubFor(get("/v3/api-docs").willReturn(ok()));

    when().get("/v3/api-docs/quality-gate-api").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/v3/api-docs")));
  }
}

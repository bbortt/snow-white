/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway;

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
import static java.lang.String.format;
import static java.lang.System.getProperty;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReportCoordinationServiceAppTest {

  @BeforeAll
  static void beforeAllSetup() {
    RestAssured.baseURI = format("http://%s", Optional.ofNullable(getProperty("api-gateway.host")).orElse("localhost"));
    RestAssured.port = parseInt(Optional.ofNullable(getProperty("api-gateway.port")).orElse("8080"));

    WireMock.configureFor(
      Optional.ofNullable(getProperty("wiremock.host")).orElse("localhost"),
      parseInt(Optional.ofNullable(getProperty("wiremock.port")).orElse("9000"))
    );
  }

  @Test
  void qualityGateCalculationRequestShouldBeForwarded() {
    var qualityGateConfigName = "report-coordination-service";

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

    when().get("/v3/api-docs/report-coordination-service").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/v3/api-docs")));
  }
}

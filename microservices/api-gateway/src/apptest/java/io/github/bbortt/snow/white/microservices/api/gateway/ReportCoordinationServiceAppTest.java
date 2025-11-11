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

  /**
   * The API gateway shall route requests for calculating Quality Gates to the report-coordinator-api service.
   * <p>
   * When the client calls {@code /api/rest/v1/quality-gates/{qualityGateConfigName}/calculate}, the gateway must forward the request to {@code /api/rest/v1/quality-gates/{qualityGateConfigName}/calculate} exactly as received.
   */
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

  /**
   * The API gateway shall route requests for all Reports to the report-coordinator-api service.
   * <p>
   * When the client calls {@code /api/rest/v1/reports}, the gateway must forward the request to {@code /api/rest/v1/reports} exactly as received.
   */
  @Test
  void reportsResourceRequestShouldBeForwarded() {
    stubFor(get("/api/rest/v1/reports").willReturn(ok()));

    when().get("/api/rest/v1/reports").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/reports")));
  }

  /**
   * The API gateway shall route requests for a specific Report to the report-coordinator-api service without modifying the path parameters.
   * <p>
   * When the client calls {@code /api/rest/v1/reports/{reportId}}, the gateway must forward the request to {@code /api/rest/v1/reports/{reportId}} exactly as received.
   */
  @Test
  void reportByIdResourceRequestShouldBeForwarded() {
    var reportId = "report-id";

    stubFor(get(urlPathTemplate("/api/rest/v1/reports/{reportId}")).withPathParam("reportId", equalTo(reportId)).willReturn(ok()));

    when().get("/api/rest/v1/reports/{reportId}", reportId).then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/reports/" + reportId)));
  }

  /**
   * The API gateway shall expose service-specific Swagger documentation under a namespaced path.
   * <p>
   * Requests to {@code /v3/api-docs/report-coordinator-api} must be forwarded to the report-coordinator-api {@code /v3/api-docs} endpoint.
   */
  @Test
  void shouldTransformSwaggerApiRequest() {
    stubFor(get("/v3/api-docs").willReturn(ok()));

    when().get("/v3/api-docs/report-coordinator-api").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/v3/api-docs")));
  }
}

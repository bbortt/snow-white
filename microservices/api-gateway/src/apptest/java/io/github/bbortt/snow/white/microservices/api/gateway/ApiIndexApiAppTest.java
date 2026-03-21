/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway;

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

import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ApiIndexApiAppTest {

  @BeforeAll
  static void beforeAllSetup() {
    RestAssured.baseURI = format("http://%s", getProperty("api-gateway.host", "localhost"));
    RestAssured.port = parseInt(getProperty("api-gateway.port", "8080"));

    WireMock.configureFor(getProperty("wiremock.host", "localhost"), parseInt(getProperty("wiremock.port", "9000")));
  }

  /**
   * The API gateway shall route requests for all APIs to the api-index-api service.
   * <p>
   * When the client calls {@code /api/rest/v1/apis}, the gateway must forward the request to {@code /api/rest/v1/apis} exactly as received.
   */
  @Test
  void apisResourceRequestShouldBeForwarded() {
    stubFor(get("/api/rest/v1/apis").willReturn(ok()));

    when().get("/api/rest/v1/apis").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/api/rest/v1/apis")));
  }

  /**
   * The API gateway shall route requests for a specific API entry to the api-index-api service without modifying path parameters.
   * <p>
   * When the client calls {@code /api/rest/v1/apis/{service}/{api}/{version}}, the gateway must forward the request to
   * {@code /api/rest/v1/apis/{service}/{api}/{version}} exactly as received.
   */
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

    verify(getRequestedFor(urlEqualTo(format("/api/rest/v1/apis/%s/%s/%s", service, api, version))));
  }

  /**
   * The API gateway shall expose service-specific Swagger documentation under a namespaced path.
   * <p>
   * Requests to {@code /v3/api-docs/api-index-api} must be forwarded to the api-index-api {@code /v3/api-docs} endpoint.
   */
  @Test
  void shouldTransformSwaggerApiRequest() {
    stubFor(get("/v3/api-docs").willReturn(ok()));

    when().get("/v3/api-docs/api-index-api").then().statusCode(200);

    verify(getRequestedFor(urlEqualTo("/v3/api-docs")));
  }
}

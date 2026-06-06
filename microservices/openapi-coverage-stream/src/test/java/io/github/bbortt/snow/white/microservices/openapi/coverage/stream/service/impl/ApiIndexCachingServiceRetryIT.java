/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT.ADMIN_TOKEN;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.Main;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClientResponseException;
import org.wiremock.spring.EnableWireMock;

@EnableWireMock
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "influxdb.org=snow-white",
    "influxdb.bucket=raw-data",
    "influxdb.token=" + ADMIN_TOKEN,
    "snow.white.openapi.coverage.stream.api-index.base-url=${wiremock.server.baseUrl}",
    "snow.white.openapi.coverage.stream.calculation-request-topic=snow-white-calculation-request",
    "snow.white.openapi.coverage.stream.openapi-calculation-response-topic=snow-white-openapi-calculation-response",
  }
)
class ApiIndexCachingServiceRetryIT extends AbstractOpenApiCoverageServiceIT {

  private static final String SERVICE_NAME = "retry-service";
  private static final String API_NAME = "retry-api";
  private static final String API_VERSION = "1.0.0";

  private static final String API_DETAILS_PATH =
    "/api/rest/v1/apis/" + SERVICE_NAME + "/" + API_NAME + "/" + API_VERSION;

  private static final ApiInformation API_INFORMATION = ApiInformation.builder()
    .serviceName(SERVICE_NAME)
    .apiName(API_NAME)
    .apiVersion(API_VERSION)
    .apiType(OPENAPI)
    .build();

  @Autowired
  private ApiIndexCachingService apiIndexCachingService;

  @BeforeEach
  void setUp() {
    reset();
  }

  @Test
  void shouldRetryBeforePropagatingFailure() {
    stubFor(get(urlEqualTo(API_DETAILS_PATH)).willReturn(serverError()));

    assertThatThrownBy(() ->
      apiIndexCachingService.fetchApiSourceUrl(API_INFORMATION)
    ).isInstanceOf(RestClientResponseException.class);

    verify(3, getRequestedFor(urlEqualTo(API_DETAILS_PATH)));
  }
}

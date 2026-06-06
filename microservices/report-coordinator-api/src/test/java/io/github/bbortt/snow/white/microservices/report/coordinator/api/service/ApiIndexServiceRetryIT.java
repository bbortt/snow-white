/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ApiIndexService.ValidationResult;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiIndexServiceRetryIT extends AbstractReportCoordinationServiceIT {

  private static final ApiTest API_TEST = ApiTest.builder()
    .serviceName("retry-service")
    .apiName("retry-api")
    .apiVersion("1.0.0")
    .apiType(OPENAPI.getVal())
    .build();

  private static final String ENDPOINT =
    "/api/rest/v1/apis/retry-service/retry-api/1.0.0";

  @Autowired
  private ApiIndexService apiIndexService;

  @BeforeEach
  void setUp() {
    reset();
    stubFor(get(urlEqualTo(ENDPOINT)).willReturn(serverError()));
  }

  @Test
  void shouldRetryUpToMaxAttemptsBeforePropagatingFailure() {
    assertThat(apiIndexService.fetchCompleteApiInformation(Set.of(API_TEST)))
      .hasSize(1)
      .allSatisfy(r ->
        assertThat(r).isInstanceOf(ValidationResult.Failure.class)
      );

    verify(3, getRequestedFor(urlEqualTo(ENDPOINT)));
  }
}

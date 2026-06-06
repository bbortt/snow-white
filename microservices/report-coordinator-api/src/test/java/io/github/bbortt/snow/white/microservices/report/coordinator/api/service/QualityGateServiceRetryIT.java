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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientResponseException;

class QualityGateServiceRetryIT extends AbstractReportCoordinationServiceIT {

  private static final String QUALITY_GATE_NAME = "retry-test";
  private static final String ENDPOINT =
    "/api/rest/v1/quality-gates/" + QUALITY_GATE_NAME;

  @Autowired
  private QualityGateService qualityGateService;

  @BeforeEach
  void setUp() {
    reset();
    stubFor(get(urlEqualTo(ENDPOINT)).willReturn(serverError()));
  }

  @Test
  void shouldRetryUpToMaxAttemptsBeforePropagatingFailure() {
    assertThatThrownBy(() ->
      qualityGateService.findQualityGateConfigByName(QUALITY_GATE_NAME)
    ).isInstanceOf(RestClientResponseException.class);

    verify(3, getRequestedFor(urlEqualTo(ENDPOINT)));
  }
}

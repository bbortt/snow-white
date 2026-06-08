/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ApiIndexCachingServiceRetryIT {

  private static final ApiInformation API_INFORMATION = ApiInformation.builder()
    .serviceName("retry-service")
    .name("retry-api")
    .version("1.0.0")
    .build();

  private static final String EXISTS_PATH =
    "/api/rest/v1/apis/retry-service/retry-api/1.0.0/exists";

  private static final String INGEST_PATH = "/api/rest/v1/apis";

  @Autowired
  private ApiIndexCachingService apiIndexCachingService;

  @BeforeEach
  void setUp() {
    reset();
  }

  @Test
  void shouldRetryAndRecoverApiInformationIndexed() {
    stubFor(get(urlPathEqualTo(EXISTS_PATH)).willReturn(serverError()));

    assertThat(
      apiIndexCachingService.apiInformationIndexed(API_INFORMATION)
    ).isFalse();

    verify(3, getRequestedFor(urlPathEqualTo(EXISTS_PATH)));
  }

  @Test
  void shouldRetryAndRecoverPublishApiInformation() {
    stubFor(post(urlEqualTo(INGEST_PATH)).willReturn(serverError()));

    assertThatCode(() ->
      apiIndexCachingService.publishApiInformation(API_INFORMATION)
    ).doesNotThrowAnyException();

    verify(3, postRequestedFor(urlEqualTo(INGEST_PATH)));
  }
}

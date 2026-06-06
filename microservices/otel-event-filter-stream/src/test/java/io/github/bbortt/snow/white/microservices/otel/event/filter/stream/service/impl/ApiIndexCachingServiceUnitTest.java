/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.service.impl;

import static java.lang.Boolean.TRUE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.client.apiindexapi.api.ApiIndexApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

@ExtendWith({ MockitoExtension.class })
class ApiIndexCachingServiceUnitTest {

  private static final String OTEL_SERVICE_NAME = "otel-service-name";
  private static final String API_NAME = "api-name";
  private static final String API_VERSION = "api-version";

  @Mock
  private ApiIndexApi apiIndexApiMock;

  private ApiIndexCachingService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiIndexCachingService(apiIndexApiMock);
  }

  @Nested
  class ApiExistsTest {

    @Test
    void shouldReturnTrue_whenApiHasBeenIndexedBefore() {
      doReturn(ResponseEntity.ok().build())
        .when(apiIndexApiMock)
        .checkApiExistsWithHttpInfo(
          OTEL_SERVICE_NAME,
          API_NAME,
          API_VERSION,
          TRUE
        );

      assertThat(
        fixture.apiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION)
      ).isTrue();
    }

    @Test
    void shouldReturnFalse_whenApiHasNotBeenIndexedBefore() {
      doReturn(ResponseEntity.notFound().build())
        .when(apiIndexApiMock)
        .checkApiExistsWithHttpInfo(
          OTEL_SERVICE_NAME,
          API_NAME,
          API_VERSION,
          TRUE
        );

      assertThat(
        fixture.apiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION)
      ).isFalse();
    }

    @Test
    void shouldRethrow_whenApiExistenceCheckFails() {
      doThrow(
        new RestClientResponseException(
          "Service Unavailable",
          SERVICE_UNAVAILABLE,
          SERVICE_UNAVAILABLE.getReasonPhrase(),
          null,
          null,
          UTF_8
        )
      )
        .when(apiIndexApiMock)
        .checkApiExistsWithHttpInfo(
          OTEL_SERVICE_NAME,
          API_NAME,
          API_VERSION,
          TRUE
        );

      assertThatThrownBy(() ->
        fixture.apiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION)
      ).isInstanceOf(RestClientResponseException.class);
    }
  }
}

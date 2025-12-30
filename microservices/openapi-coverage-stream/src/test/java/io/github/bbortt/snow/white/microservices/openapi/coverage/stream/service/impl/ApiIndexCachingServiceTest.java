/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class ApiIndexCachingServiceTest {

  private static final String SERVICE_NAME = "test-service";
  private static final String API_NAME = "Test API";
  private static final String API_VERSION = "1.0.0";

  @Mock
  private ApiIndexApi apiIndexApiMock;

  private ApiIndexCachingService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiIndexCachingService(apiIndexApiMock);
  }

  @Nested
  class FetchApiSourceUrlTest {

    @Test
    void shouldFetchAndReturnSourceUrl() throws OpenApiNotIndexedException {
      var sourceUrl = "sourceUrl";
      var dto = new GetAllApis200ResponseInner().sourceUrl(sourceUrl);

      doReturn(ResponseEntity.ok(dto))
        .when(apiIndexApiMock)
        .getApiDetailsWithHttpInfo(SERVICE_NAME, API_NAME, API_VERSION);

      var result = fixture.fetchApiSourceUrl(
        ApiInformation.builder()
          .serviceName(SERVICE_NAME)
          .apiName(API_NAME)
          .apiVersion(API_VERSION)
          .build()
      );

      assertThat(result).isEqualTo(sourceUrl);
    }

    static Stream<
      ResponseEntity<GetAllApis200ResponseInner>
    > shouldThrow_whenFetchFails() {
      return Stream.of(
        ResponseEntity.internalServerError().build(),
        ResponseEntity.ok().build()
      );
    }

    @MethodSource
    @ParameterizedTest
    void shouldThrow_whenFetchFails(
      ResponseEntity<GetAllApis200ResponseInner> responseEntity
    ) {
      doReturn(responseEntity)
        .when(apiIndexApiMock)
        .getApiDetailsWithHttpInfo(SERVICE_NAME, API_NAME, API_VERSION);

      var apiInformation = ApiInformation.builder()
        .serviceName(SERVICE_NAME)
        .apiName(API_NAME)
        .apiVersion(API_VERSION)
        .build();

      assertThatThrownBy(() ->
        fixture.fetchApiSourceUrl(apiInformation)
      ).isInstanceOf(OpenApiNotIndexedException.class);
    }
  }
}

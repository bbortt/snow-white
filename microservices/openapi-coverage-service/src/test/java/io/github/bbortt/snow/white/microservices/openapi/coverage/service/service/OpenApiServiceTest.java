/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.redis.ApiEndpointEntry;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.redis.ApiEndpointRepository;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.exception.UnparseableOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiServiceTest {

  @Mock
  private ApiEndpointRepository apiEndpointRepositoryMock;

  private OpenApiService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiService(apiEndpointRepositoryMock);
  }

  @Nested
  class FindAndParseOpenApi {

    private static final String OTEL_SERVICE_NAME = "service";
    private static final String API_NAME = "api";
    private static final String API_VERSION = "1.2.3";

    private ApiInformation apiInformation;

    @BeforeEach
    void beforeEachSetup() {
      apiInformation = ApiInformation.builder()
        .serviceName(OTEL_SERVICE_NAME)
        .apiName(API_NAME)
        .apiVersion(API_VERSION)
        .build();
    }

    @Test
    void shouldQueryAndParseOpenApi()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var apiEndpointEntry = new ApiEndpointEntry(
        OTEL_SERVICE_NAME,
        API_NAME,
        API_VERSION,
        requireNonNull(
          getClass()
            .getClassLoader()
            .getResource("OpenApiServiceTest/swagger.yaml")
        ).toExternalForm(),
        OPENAPI
      );

      doReturn(Optional.of(apiEndpointEntry))
        .when(apiEndpointRepositoryMock)
        .findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
          OTEL_SERVICE_NAME,
          API_NAME,
          API_VERSION
        );

      OpenAPI openAPI = fixture.findAndParseOpenApi(apiInformation);

      assertThat(openAPI)
        .isNotNull()
        .extracting(a -> a.getInfo().getTitle())
        .isEqualTo("Swagger Petstore - OpenAPI 3.0");
    }

    @Test
    void shouldThrow_whenApiIsUnparseable() {
      var apiEndpointEntry = new ApiEndpointEntry(
        OTEL_SERVICE_NAME,
        API_NAME,
        API_VERSION,
        requireNonNull(
          getClass()
            .getClassLoader()
            .getResource("OpenApiServiceTest/swagger-invalid.yaml")
        ).toExternalForm(),
        OPENAPI
      );

      doReturn(Optional.of(apiEndpointEntry))
        .when(apiEndpointRepositoryMock)
        .findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
          OTEL_SERVICE_NAME,
          API_NAME,
          API_VERSION
        );

      assertThatThrownBy(() -> fixture.findAndParseOpenApi(apiInformation))
        .isInstanceOf(UnparseableOpenApiException.class)
        .hasMessageStartingWith("Unparsable OpenAPI");
    }

    @Test
    void shouldThrow_whenApiEndpointNotFound() {
      assertThatThrownBy(() ->
        fixture.findAndParseOpenApi(apiInformation)
      ).isInstanceOf(OpenApiNotIndexedException.class);

      verify(
        apiEndpointRepositoryMock
      ).findByOtelServiceNameEqualsAndApiNameEqualsAndApiVersionEquals(
        OTEL_SERVICE_NAME,
        API_NAME,
        API_VERSION
      );
    }
  }
}

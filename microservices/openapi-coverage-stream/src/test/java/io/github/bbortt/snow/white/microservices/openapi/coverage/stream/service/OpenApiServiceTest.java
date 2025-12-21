/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiServiceTest {

  @Mock
  private CachingService cachingServiceMock;

  private OpenApiService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiService(cachingServiceMock);
  }

  @Nested
  class FindAndParseOpenApi {

    private ApiInformation apiInformation;

    @BeforeEach
    void beforeEachSetup() {
      apiInformation = ApiInformation.builder()
        .serviceName("serviceName")
        .apiName("apiName")
        .apiVersion("apiVersion")
        .build();
    }

    @Test
    void shouldQueryAndParseOpenApi()
      throws OpenApiNotIndexedException, UnparseableOpenApiException {
      var sourceUrl = requireNonNull(
        getClass()
          .getClassLoader()
          .getResource("OpenApiServiceTest/swagger.yaml")
      ).toExternalForm();

      doReturn(sourceUrl)
        .when(cachingServiceMock)
        .fetchApiSourceUrl(apiInformation);

      OpenAPI openAPI = fixture.findAndParseOpenApi(apiInformation);

      assertThat(openAPI)
        .isNotNull()
        .extracting(a -> a.getInfo().getTitle())
        .isEqualTo("Swagger Petstore - OpenAPI 3.0");
    }

    @Test
    void shouldThrow_whenApiIsUnparseable() throws OpenApiNotIndexedException {
      var sourceUrl = requireNonNull(
        getClass()
          .getClassLoader()
          .getResource("OpenApiServiceTest/swagger-invalid.yaml")
      ).toExternalForm();

      doReturn(sourceUrl)
        .when(cachingServiceMock)
        .fetchApiSourceUrl(apiInformation);

      assertThatThrownBy(() -> fixture.findAndParseOpenApi(apiInformation))
        .isInstanceOf(UnparseableOpenApiException.class)
        .hasMessageStartingWith("Unparsable OpenAPI");
    }
  }
}

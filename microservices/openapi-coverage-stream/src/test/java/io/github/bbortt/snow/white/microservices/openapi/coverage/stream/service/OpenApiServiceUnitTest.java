/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.OpenApiNotIndexedException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.UnparseableOpenApiException;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client.OpenApiSourceClient;
import io.swagger.v3.oas.models.OpenAPI;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiServiceUnitTest {

  @Mock
  private CachingService cachingServiceMock;

  @Mock
  private OpenApiSourceClient openApiSourceClientMock;

  @InjectMocks
  private OpenApiService fixture;

  @Nested
  class FindAndParseOpenApiTest {

    private ApiInformation apiInformation;

    @BeforeEach
    void beforeEachSetup() {
      apiInformation = defaultApiInformation();
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

    @Test
    void shouldFetchRemoteLocationsThroughRetryableClient()
      throws OpenApiNotIndexedException, UnparseableOpenApiException, IOException {
      var sourceUrl = "https://example.test/openapi/swagger.yaml";
      var specContent = new String(
        requireNonNull(
          getClass()
            .getClassLoader()
            .getResourceAsStream("OpenApiServiceTest/swagger.yaml")
        ).readAllBytes(),
        UTF_8
      );

      doReturn(sourceUrl)
        .when(cachingServiceMock)
        .fetchApiSourceUrl(apiInformation);
      doReturn(specContent)
        .when(openApiSourceClientMock)
        .fetchContent(sourceUrl);

      OpenAPI openAPI = fixture.findAndParseOpenApi(apiInformation);

      assertThat(openAPI)
        .isNotNull()
        .extracting(a -> a.getInfo().getTitle())
        .isEqualTo("Swagger Petstore - OpenAPI 3.0");
    }

    @Test
    void shouldThrowUnparseableOpenApiException_whenRemoteFetchFails()
      throws OpenApiNotIndexedException, IOException {
      var sourceUrl = "https://example.test/openapi/swagger.yaml";

      doReturn(sourceUrl)
        .when(cachingServiceMock)
        .fetchApiSourceUrl(apiInformation);
      doThrow(new IOException("Connection refused"))
        .when(openApiSourceClientMock)
        .fetchContent(sourceUrl);

      assertThatThrownBy(() -> fixture.findAndParseOpenApi(apiInformation))
        .isInstanceOf(UnparseableOpenApiException.class)
        .hasMessageContaining("Unable to read location `" + sourceUrl + "`")
        .hasMessageContaining("Connection refused");
    }
  }
}

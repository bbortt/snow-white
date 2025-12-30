/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.dto.GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI;
import static io.github.bbortt.snow.white.microservices.api.sync.job.util.TestUtils.getResourceContent;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
@IntegrationTest
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.sync.job.api-index.base-url=${wiremock.server.baseUrl}",
    "snow.white.api.sync.job.service-interface.base-url=${wiremock.server.baseUrl}",
    "snow.white.api.sync.job.service-interface.index-uri=/sir/index",
  }
)
class SyncJobIT {

  @Value("${wiremock.server.baseUrl}")
  private String wiremockServerBaseUrl;

  @Autowired
  private SyncJob fixture;

  public static Stream<String> indexContentTypes() {
    return Stream.of(APPLICATION_JSON_VALUE, APPLICATION_OCTET_STREAM_VALUE);
  }

  @ParameterizedTest
  @MethodSource("indexContentTypes")
  void shouldSyncNewApisInCatalog(String contentType) throws IOException {
    var sirResponse = getResourceContent(
      "sir/full-example-response.json"
    ).replace("${wiremock.server.baseUrl}", wiremockServerBaseUrl);

    stubFor(
      get("/sir/index").willReturn(okForContentType(contentType, sirResponse))
    );

    stubFor(
      post("/api/rest/v1/apis")
        .withRequestBody(
          equalToJson(
            JsonMapper.shared().writeValueAsString(
              new GetAllApis200ResponseInner()
                .serviceName("example-application")
                .apiName("Swagger Petstore - OpenAPI 3.1")
                .apiVersion("1.2.3")
                .sourceUrl(format("%s/sir/petstore.yml", wiremockServerBaseUrl))
                .apiType(OPENAPI)
            )
          )
        )
        .willReturn(created())
    );

    assertDoesNotThrow(() -> fixture.syncCatalog());

    verify(
      getRequestedFor(
        urlEqualTo(
          "/api/rest/v1/apis/example-application/Swagger%20Petstore%20-%20OpenAPI%203.1/1.2.3/exists"
        )
      )
    );
  }

  @ParameterizedTest
  @MethodSource("indexContentTypes")
  void shouldSkipSync_whenApiIsAlreadyIndexed(String contentType)
    throws IOException {
    var sirResponse = getResourceContent(
      "sir/full-example-response.json"
    ).replace("${wiremock.server.baseUrl}", wiremockServerBaseUrl);

    stubFor(
      get("/sir/index").willReturn(okForContentType(contentType, sirResponse))
    );

    stubFor(
      get(
        "/api/rest/v1/apis/example-application/Swagger%20Petstore%20-%20OpenAPI%203.1/1.2.3/exists"
      ).willReturn(ok())
    );

    verify(0, getRequestedFor(urlEqualTo("/api/rest/v1/apis")));
  }
}

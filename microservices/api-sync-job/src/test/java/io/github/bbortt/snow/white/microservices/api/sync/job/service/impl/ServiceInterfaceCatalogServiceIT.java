/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static io.github.bbortt.snow.white.microservices.api.sync.job.util.TestUtils.getResourceContent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import io.github.bbortt.snow.white.microservices.api.sync.job.Main;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@IntegrationTest
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.sync.job.service-interface.base-url=${wiremock.server.baseUrl}",
    "snow.white.api.sync.job.service-interface.index-uri=/sir/index",
  }
)
class ServiceInterfaceCatalogServiceIT extends AbstractApiCatalogServiceIT {

  @Value("${wiremock.server.baseUrl}")
  private String wiremockServerBaseUrl;

  @Autowired
  private ServiceInterfaceCatalogService fixture;

  @Nested
  class FetchApiIndex {

    public static Stream<String> indexContentTypes() {
      return Stream.of(APPLICATION_JSON_VALUE, APPLICATION_OCTET_STREAM_VALUE);
    }

    @ParameterizedTest
    @MethodSource("indexContentTypes")
    void shouldSyncCatalog(String contentType) throws IOException {
      var exampleResponse = getResourceContent(
        "sir/full-example-response.json"
      ).replace("${wiremock.server.baseUrl}", wiremockServerBaseUrl);

      stubFor(
        get("/sir/index").willReturn(
          okForContentType(contentType, exampleResponse)
        )
      );

      var apiInformation = fetchApiIndexAndAssertBasicInformation(fixture);
      assertThat(apiInformation.getSourceUrl()).isEqualTo(
        wiremockServerBaseUrl + "/sir/petstore.yml"
      );
    }
  }
}

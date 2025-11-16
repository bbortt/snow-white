/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.and;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.bbortt.snow.white.microservices.api.sync.job.util.TestUtils.getResourceContent;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import io.github.bbortt.snow.white.microservices.api.sync.job.Main;
import java.io.IOException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@IntegrationTest
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.sync.job.api-index.base-url=http://localhost:8085",
    "snow.white.api.sync.job.backstage.base-url=${wiremock.server.baseUrl}",
    "snow.white.api.sync.job.minio.bucket-name=backstage-catalog-service-it",
    "snow.white.api.sync.job.minio.init-bucket=true",
    "snow.white.api.sync.job.minio.access-key=minioadmin",
    "snow.white.api.sync.job.minio.secret-key=minioadmin",
  }
)
public class BackstageCatalogServiceIT extends AbstractApiCatalogServiceIT {

  private static final int MINIO_S3_PORT = 9000;

  @Container
  static final MinIOContainer MINIO_CONTAINER = new MinIOContainer(
    "minio/minio:latest-cicd"
  ).withExposedPorts(MINIO_S3_PORT);

  @DynamicPropertySource
  static void minioProperties(DynamicPropertyRegistry registry) {
    registry.add("snow.white.api.sync.job.minio.endpoint", () ->
      format(
        "http://%s:%S",
        MINIO_CONTAINER.getHost(),
        MINIO_CONTAINER.getMappedPort(MINIO_S3_PORT)
      )
    );
  }

  @Autowired
  private BackstageCatalogService fixture;

  @Nested
  class FetchApiIndex {

    @Test
    void shouldSyncCatalog() throws IOException {
      var paginationInformation = getResourceContent(
        "backstage/pagination-information.json"
      );

      stubFor(
        get(urlPathEqualTo("/entities/by-query"))
          .withQueryParam("limit", equalTo("0"))
          .willReturn(
            okForContentType(APPLICATION_JSON_VALUE, paginationInformation)
          )
      );

      var specResponse = getResourceContent("backstage/by-query.json");

      stubFor(
        get(urlPathEqualTo("/entities/by-query"))
          .withQueryParam(
            "fields",
            and(
              containing("metadata.annotations"),
              containing("spec.definition")
            )
          )
          .withQueryParam("limit", equalTo("10"))
          .withQueryParam("offset", equalTo("0"))
          .willReturn(okForContentType(APPLICATION_JSON_VALUE, specResponse))
      );

      var apiInformation = fetchApiIndexAndAssertBasicInformation(fixture);
      assertThat(apiInformation.getSourceUrl()).isNotEmpty();
    }
  }
}

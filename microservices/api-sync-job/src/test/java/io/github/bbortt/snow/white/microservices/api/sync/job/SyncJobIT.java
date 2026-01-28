/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.bbortt.snow.white.microservices.api.sync.job.SyncJobIT.ARTIFACTORY_BEARER_TOKEN;
import static io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.dto.GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.bbortt.snow.white.microservices.api.sync.job.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
@IntegrationTest
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.sync.job.api-index.base-url=${wiremock.server.baseUrl}",
    "snow.white.api.sync.job.artifactory.base-url=${wiremock.server.baseUrl}",
    "snow.white.api.sync.job.artifactory.access-token=" +
      ARTIFACTORY_BEARER_TOKEN,
    "snow.white.api.sync.job.artifactory.repository=" +
      SyncJobIT.ARTIFACTORY_REPOSITORY,
  }
)
class SyncJobIT {

  public static final String ARTIFACTORY_BEARER_TOKEN = "private-token";
  public static final String ARTIFACTORY_REPOSITORY =
    "snow-white-generic-local";

  @Autowired
  private SyncJob fixture;

  @Test
  void shouldSyncNewApisInCatalog() {
    createStubsForArtifactory();

    stubFor(
      get(
        "/api/rest/v1/apis/example-application/Petstore%20API/1.0.0/exists"
      ).willReturn(notFound())
    );

    stubFor(
      get(
        "/api/rest/v1/apis/example-application/Users%20API/2.0.0/exists"
      ).willReturn(notFound())
    );

    stubFor(
      post("/api/rest/v1/apis")
        .withRequestBody(
          equalToJson(
            JsonMapper.shared().writeValueAsString(
              new GetAllApis200ResponseInner()
                .serviceName("example-application")
                .apiName("Petstore API")
                .apiVersion("1.0.0")
                .sourceUrl(
                  "http://localhost:3000/artifactory/snow-white-generic-local/petstore.json"
                )
                .apiType(OPENAPI)
            )
          )
        )
        .willReturn(created())
    );

    stubFor(
      post("/api/rest/v1/apis")
        .withRequestBody(
          equalToJson(
            JsonMapper.shared().writeValueAsString(
              new GetAllApis200ResponseInner()
                .serviceName("example-application")
                .apiName("Users API")
                .apiVersion("2.0.0")
                .sourceUrl(
                  "http://localhost:3000/artifactory/snow-white-generic-local/users.yaml"
                )
                .apiType(OPENAPI)
            )
          )
        )
        .willReturn(created())
    );

    assertDoesNotThrow(() -> fixture.syncCatalog());

    verify(2, postRequestedFor(urlEqualTo("/api/rest/v1/apis")));
  }

  private static void createStubsForArtifactory() {
    stubForAqlQueryPost(
      "*.json",
      "{\"repo\": \"%s\", \"path\": \".\", \"name\": \"petstore.json\"}"
    );
    stubForAqlQueryPost("*.yml", "");
    stubForAqlQueryPost(
      "*.yaml",
      "{\"repo\": \"%s\", \"path\": \".\", \"name\": \"users.yaml\"}"
    );

    stubForArtefactGet(
      "petstore.json", // language=json
      """
      {
        "openapi": "3.0.0",
        "info": {
            "title": "Petstore API",
             "version": "1.0.0",
             "extensions": {
             "x-service-name": "example-application"
             }
         }
      }
      """
    );
    stubForArtefactGet(
      "users.yaml", // language=yaml
      """
      openapi: 3.1.0
      info:
        title: Users API
        version: 2.0.0
        extensions:
            x-service-name: example-application
      """
    );

    stubForFileInfoGet("petstore.json");
    stubForFileInfoGet("users.yaml");
  }

  private static void stubForAqlQueryPost(
    String fileExtensionPattern,
    String resultsTemplate
  ) {
    stubFor(
      post(urlPathEqualTo("/api/search/aql"))
        .withHeader(
          "Authorization",
          equalTo("Bearer " + ARTIFACTORY_BEARER_TOKEN)
        )
        .withRequestBody(
          equalTo(
            "items.find({\"$or\":[{\"$and\":[{\"repo\":\"snow-white-generic-local\",\"path\":{\"$match\":\"*\"},\"name\":{\"$match\":\"%s\"}}]}]}).include(\"name\",\"repo\",\"path\",\"actual_md5\",\"actual_sha1\",\"size\",\"type\",\"modified\",\"created\",\"property\")".formatted(
              fileExtensionPattern
            )
          )
        )
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              // language=json
              """
              {
                "results": [
                  %s
                ]
              }
              """.formatted(resultsTemplate.formatted(ARTIFACTORY_REPOSITORY))
            )
        )
    );
  }

  private static void stubForArtefactGet(String fileName, String responseBody) {
    stubFor(
      get(urlPathEqualTo("/" + ARTIFACTORY_REPOSITORY + "/" + fileName))
        .withHeader(
          "Authorization",
          equalTo("Bearer " + ARTIFACTORY_BEARER_TOKEN)
        )
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader(
              "Content-Type",
              "application/" + fileName.split("\\.")[1]
            )
            .withBody(responseBody)
        )
    );
  }

  private static void stubForFileInfoGet(String fileName) {
    stubFor(
      get(
        urlPathEqualTo(
          "/api/storage/" + ARTIFACTORY_REPOSITORY + "/" + fileName
        )
      )
        .withHeader(
          "Authorization",
          equalTo("Bearer " + ARTIFACTORY_BEARER_TOKEN)
        )
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "downloadUri": "http://localhost:3000/artifactory/%s/%s"
              }
              """.formatted(ARTIFACTORY_REPOSITORY, fileName)
            )
        )
    );
  }

  @Test
  void shouldSkipSync_whenApiIsAlreadyIndexed() {
    createStubsForArtifactory();

    stubFor(
      get(
        "/api/rest/v1/apis/example-application/Petstore%20API/1.0.0/exists"
      ).willReturn(ok())
    );

    stubFor(
      get(
        "/api/rest/v1/apis/example-application/Users%20API/2.0.0/exists"
      ).willReturn(ok())
    );

    assertDoesNotThrow(() -> fixture.syncCatalog());

    verify(0, postRequestedFor(urlEqualTo("/api/rest/v1/apis")));
  }
}

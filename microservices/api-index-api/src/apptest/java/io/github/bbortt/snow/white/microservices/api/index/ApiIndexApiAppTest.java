/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index;

import static io.github.bbortt.snow.white.microservices.api.index.CitrusUtils.getHttpEndpoint;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis500Response;
import java.util.List;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.apiindex.api.ApiIndexApi;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@CitrusSupport
class ApiIndexApiAppTest {

  private static ApiIndexApi apiIndexApi;

  @BeforeAll
  static void beforeAllSetup() {
    apiIndexApi = new ApiIndexApi(
      getHttpEndpoint(
        getProperty("api-index-api.host", "localhost"),
        parseInt(getProperty("api-index-api.port", "8085"))
      )
    );
  }

  /**
   * Verifies that the APIs listing endpoint accepts pagination parameters and returns HTTP 200.
   *
   * <p>
   * When requesting {@code GET /api/rest/v1/apis?page=0&size=5}, the service must:
   * <ul>
   *   <li>return HTTP 200 OK</li>
   *   <li>return a valid JSON array (empty or populated)</li>
   * </ul>
   *
   * <p>
   * Note: the {@code sort} parameter is intentionally omitted because Citrus currently attempts to
   * parse its value as JSON, causing the request to fail.
   */
  @Test
  @CitrusTest
  void shouldGetAllApisWithPaginationParameters(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(apiIndexApi.sendGetAllApis().page(0).size(5));

    testRunner.then(
      apiIndexApi
        .receiveGetAllApis(OK)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var apis = JsonMapper.shared().readValue(
            payload,
            new TypeReference<List<GetAllApis200ResponseInner>>() {}
          );
          assertThat(apis).isNotNull();
        })
    );
  }

  /**
   * Verifies that a valid stable API specification can be ingested and the service responds with
   * HTTP 201.
   *
   * <p>
   * When sending {@code POST /api/rest/v1/apis} with a JSON body containing all required fields
   * and {@code prerelease: false}, the service must:
   * <ul>
   *   <li>return HTTP 201 Created</li>
   *   <li>return an empty response body</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldIngestApi(@CitrusResource TestCaseRunner testRunner) {
    var payload = JsonMapper.shared().writeValueAsString(
      GetAllApis200ResponseInner.builder()
        .serviceName("shouldIngestApi")
        .apiName("shouldIngestApi-api")
        .apiVersion("1.0.0")
        .sourceUrl("https://example.com/shouldIngestApi.yaml")
        .apiType(GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI)
        .build()
    );

    testRunner.when(
      apiIndexApi.sendIngestApi().getMessageBuilderSupport().body(payload)
    );
    testRunner.then(apiIndexApi.receiveIngestApi(CREATED));
  }

  /**
   * Verifies that submitting an empty body to the ingest endpoint results in HTTP 400.
   *
   * <p>
   * When sending {@code POST /api/rest/v1/apis} with body {@code {}} (all required fields absent),
   * the service must:
   * <ul>
   *   <li>return HTTP 400 Bad Request</li>
   *   <li>return a JSON error body whose {@code code} equals {@code "Bad Request"}</li>
   * </ul>
   *
   * <p>
   * Schema validation is disabled on the send side so the intentionally invalid body is not
   * rejected by Citrus before it reaches the server.
   */
  @Test
  @CitrusTest
  void shouldReturn400WhenIngestingApiWithMissingRequiredFields(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      apiIndexApi
        .sendIngestApi()
        .schemaValidation(false)
        .getMessageBuilderSupport()
        .body("{}")
    );

    testRunner.then(
      apiIndexApi
        .receiveIngestApi(BAD_REQUEST)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var error = JsonMapper.shared().readValue(
            payload,
            GetAllApis500Response.class
          );
          assertThat(error.getCode()).isEqualTo(BAD_REQUEST.getReasonPhrase());
          assertThat(error.getMessage()).isNotBlank();
        })
    );
  }

  /**
   * Verifies that ingesting a stable (non-prerelease) API with a {@code content} field results in
   * HTTP 400.
   *
   * <p>
   * When sending {@code POST /api/rest/v1/apis} with {@code prerelease: false} (the default) and a
   * non-blank {@code content} value, the service must:
   * <ul>
   *   <li>return HTTP 400 Bad Request</li>
   *   <li>return a JSON error body whose {@code code} equals {@code "Bad Request"} and whose
   *       {@code message} is non-blank</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn400WhenIngestingStableApiWithContent(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var payload = JsonMapper.shared().writeValueAsString(
      GetAllApis200ResponseInner.builder()
        .serviceName("shouldReturn400WhenIngestingStableApiWithContent")
        .apiName("shouldReturn400WhenIngestingStableApiWithContent-api")
        .apiVersion("1.0.0")
        .sourceUrl("https://example.com/api.yaml")
        .apiType(GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI)
        .prerelease(false)
        .content("openapi: 3.1.0")
        .build()
    );

    testRunner.when(
      apiIndexApi.sendIngestApi().getMessageBuilderSupport().body(payload)
    );

    testRunner.then(
      apiIndexApi
        .receiveIngestApi(BAD_REQUEST)
        .validate((message, context) -> {
          var payload2 = message.getPayload(String.class);
          assertThat(payload2).isNotEmpty();

          var error = JsonMapper.shared().readValue(
            payload2,
            GetAllApis500Response.class
          );
          assertThat(error.getCode()).isEqualTo(BAD_REQUEST.getReasonPhrase());
          assertThat(error.getMessage()).isNotBlank();
        })
    );
  }

  /**
   * Verifies that ingesting a stable API whose identity already exists results in HTTP 409.
   *
   * <p>
   * Given a stable API that has already been successfully ingested (HTTP 201), when sending a
   * second {@code POST /api/rest/v1/apis} with the same {@code serviceName}, {@code apiName}, and
   * {@code apiVersion}, the service must:
   * <ul>
   *   <li>return HTTP 409 Conflict</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn409WhenIngestingDuplicateApi(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var payload = JsonMapper.shared().writeValueAsString(
      GetAllApis200ResponseInner.builder()
        .serviceName("shouldReturn409WhenIngestingDuplicateApi")
        .apiName("shouldReturn409WhenIngestingDuplicateApi-api")
        .apiVersion("1.0.0")
        .sourceUrl("https://example.com/api.yaml")
        .apiType(GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI)
        .build()
    );

    testRunner.when(
      apiIndexApi.sendIngestApi().getMessageBuilderSupport().body(payload)
    );
    testRunner.then(apiIndexApi.receiveIngestApi(CREATED));

    testRunner.when(
      apiIndexApi.sendIngestApi().getMessageBuilderSupport().body(payload)
    );
    testRunner.then(apiIndexApi.receiveIngestApi(CONFLICT));
  }

  /**
   * Verifies that an ingested API can be retrieved by its identity and returns correct data.
   *
   * <p>
   * Given an API ingested via {@code POST /api/rest/v1/apis} (HTTP 201), when sending
   * {@code GET /api/rest/v1/apis/{serviceName}/{apiName}/{apiVersion}}, the service must:
   * <ul>
   *   <li>return HTTP 200 OK</li>
   *   <li>return a JSON body matching the ingested {@code serviceName}, {@code apiName}, and
   *       {@code apiVersion}</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldGetApiDetails(@CitrusResource TestCaseRunner testRunner) {
    var serviceName = "shouldGetApiDetails";
    var apiName = "shouldGetApiDetails-api";
    var apiVersion = "1.0.0";

    var payload = JsonMapper.shared().writeValueAsString(
      GetAllApis200ResponseInner.builder()
        .serviceName(serviceName)
        .apiName(apiName)
        .apiVersion(apiVersion)
        .sourceUrl("https://example.com/api.yaml")
        .apiType(GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI)
        .build()
    );

    testRunner.when(
      apiIndexApi.sendIngestApi().getMessageBuilderSupport().body(payload)
    );
    testRunner.then(apiIndexApi.receiveIngestApi(CREATED));

    testRunner.when(
      apiIndexApi.sendGetApiDetails(serviceName, apiName, apiVersion)
    );
    testRunner.then(
      apiIndexApi
        .receiveGetApiDetails(OK)
        .validate((message, context) -> {
          var responsePayload = message.getPayload(String.class);
          assertThat(responsePayload).isNotEmpty();

          var apiInfo = JsonMapper.shared().readValue(
            responsePayload,
            GetAllApis200ResponseInner.class
          );
          assertThat(apiInfo.getServiceName()).isEqualTo(serviceName);
          assertThat(apiInfo.getApiName()).isEqualTo(apiName);
          assertThat(apiInfo.getApiVersion()).isEqualTo(apiVersion);
        })
    );
  }

  /**
   * Verifies that requesting API details for an unknown identity results in HTTP 404.
   *
   * <p>
   * When sending {@code GET /api/rest/v1/apis/non-existent-service/non-existent-api/0.0.0}, the
   * service must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenApiDetailsNotFound(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      apiIndexApi.sendGetApiDetails(
        "non-existent-service",
        "non-existent-api",
        "0.0.0"
      )
    );
    testRunner.then(apiIndexApi.receiveGetApiDetails(NOT_FOUND));
  }

  /**
   * Verifies that a previously ingested API is detected by the existence check endpoint.
   *
   * <p>
   * Given an API ingested via {@code POST /api/rest/v1/apis} (HTTP 201), when sending
   * {@code GET /api/rest/v1/apis/{serviceName}/{apiName}/{apiVersion}/exists}, the service must:
   * <ul>
   *   <li>return HTTP 200 OK</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldCheckApiExistsAfterIngestion(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var serviceName = "shouldCheckApiExistsAfterIngestion";
    var apiName = "shouldCheckApiExistsAfterIngestion-api";
    var apiVersion = "1.0.0";

    var payload = JsonMapper.shared().writeValueAsString(
      GetAllApis200ResponseInner.builder()
        .serviceName(serviceName)
        .apiName(apiName)
        .apiVersion(apiVersion)
        .sourceUrl("https://example.com/api.yaml")
        .apiType(GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI)
        .build()
    );

    testRunner.when(
      apiIndexApi.sendIngestApi().getMessageBuilderSupport().body(payload)
    );
    testRunner.then(apiIndexApi.receiveIngestApi(CREATED));

    testRunner.when(
      apiIndexApi.sendCheckApiExists(serviceName, apiName, apiVersion)
    );
    testRunner.then(apiIndexApi.receiveCheckApiExists(OK));
  }

  /**
   * Verifies that the existence check for an unknown API identity returns HTTP 404.
   *
   * <p>
   * When sending
   * {@code GET /api/rest/v1/apis/non-existent-service/non-existent-api/0.0.0/exists}, the service
   * must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenApiDoesNotExist(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      apiIndexApi.sendCheckApiExists(
        "non-existent-service",
        "non-existent-api",
        "0.0.0"
      )
    );
    testRunner.then(apiIndexApi.receiveCheckApiExists(NOT_FOUND));
  }

  /**
   * Verifies that the raw content of a prerelease API can be retrieved after ingestion.
   *
   * <p>
   * Given a prerelease API ingested with {@code content: "openapi: 3.1.0 ..."} via
   * {@code POST /api/rest/v1/apis} (HTTP 201), when sending
   * {@code GET /api/rest/v1/apis/{serviceName}/{apiName}/{apiVersion}/raw}, the service must:
   * <ul>
   *   <li>return HTTP 200 OK</li>
   *   <li>return a body containing the raw OpenAPI content</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldGetRawApiContent(@CitrusResource TestCaseRunner testRunner) {
    var serviceName = "shouldGetRawApiContent";
    var apiName = "shouldGetRawApiContent-api";
    var apiVersion = "0.1.0-SNAPSHOT";
    var rawContent =
      "openapi: 3.1.0\ninfo:\n  title: Test API\n  version: 0.1.0";

    var payload = JsonMapper.shared().writeValueAsString(
      GetAllApis200ResponseInner.builder()
        .serviceName(serviceName)
        .apiName(apiName)
        .apiVersion(apiVersion)
        .sourceUrl("https://example.com/api.yaml")
        .apiType(GetAllApis200ResponseInner.ApiTypeEnum.OPENAPI)
        .prerelease(true)
        .content(rawContent)
        .build()
    );

    testRunner.when(
      apiIndexApi.sendIngestApi().getMessageBuilderSupport().body(payload)
    );
    testRunner.then(apiIndexApi.receiveIngestApi(CREATED));

    testRunner.when(
      apiIndexApi.sendGetRawApiContent(serviceName, apiName, apiVersion)
    );
    testRunner.then(
      apiIndexApi
        .receiveGetRawApiContent(OK)
        .validate((message, context) -> {
          var responsePayload = message.getPayload(String.class);
          assertThat(responsePayload).contains("openapi:");
        })
    );
  }

  /**
   * Verifies that requesting raw content for an unknown API identity results in HTTP 404.
   *
   * <p>
   * When sending
   * {@code GET /api/rest/v1/apis/non-existent-service/non-existent-api/0.0.0/raw}, the service
   * must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenRawApiContentNotFound(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      apiIndexApi.sendGetRawApiContent(
        "non-existent-service",
        "non-existent-api",
        "0.0.0"
      )
    );
    testRunner.then(apiIndexApi.receiveGetRawApiContent(NOT_FOUND));
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.CitrusUtils.getHttpEndpoint;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate400Response;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.qualitygate.api.QualityGateApi;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@CitrusSupport
class QualityGateApiAppTest {

  // language=json
  private static final String VALID_REQUEST_BODY = """
    {
      "includeApis": [
        {
          "serviceName": "test-service",
          "apiName": "test-api",
          "apiVersion": "1.0.0"
        }
      ]
    }
    """;

  private static QualityGateApi qualityGateApi;

  @BeforeAll
  static void beforeAllSetup() {
    configureFor(
      getProperty("wiremock.host", "localhost"),
      parseInt(getProperty("wiremock.port", "9000"))
    );

    stubFor(
      get(
        urlPathEqualTo("/api/rest/v1/apis/test-service/test-api/1.0.0")
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "serviceName": "test-service",
              "apiName": "test-api",
              "apiVersion": "1.0.0",
              "sourceUrl": "https://example.com/api.yaml",
              "apiType": "OPENAPI"
            }
            """
          )
      )
    );

    stubFor(
      get(
        urlPathEqualTo("/api/rest/v1/quality-gates/test-quality-gate")
      ).willReturn(
        aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(
            """
            {
              "name": "test-quality-gate",
              "isPredefined": false,
              "minCoveragePercentage": 80,
              "openApiCoverageCriteria": []
            }
            """
          )
      )
    );

    qualityGateApi = new QualityGateApi(
      getHttpEndpoint(
        getProperty("report-coordinator-api.host", "localhost"),
        parseInt(getProperty("report-coordinator-api.port", "8084"))
      )
    );
  }

  /**
   * Verifies that an empty body to the calculate endpoint results in HTTP 400.
   *
   * <p>
   * When sending {@code POST /api/rest/v1/quality-gates/{name}/calculate} with body {@code {}}
   * (missing the required {@code includeApis} field), the service must:
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
  void shouldReturn400WhenCalculatingWithMissingRequiredFields(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate("test-quality-gate")
        .schemaValidation(false)
        .getMessageBuilderSupport()
        .body("{}")
    );

    testRunner.then(
      qualityGateApi
        .receiveCalculateQualityGate(BAD_REQUEST)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var error = JsonMapper.shared().readValue(
            payload,
            CalculateQualityGate400Response.class
          );
          assertThat(error.getCode()).isEqualTo(BAD_REQUEST.getReasonPhrase());
          assertThat(error.getMessage()).isNotBlank();
        })
    );
  }

  /**
   * Verifies that a calculate request whose APIs are not in the index results in HTTP 400.
   *
   * <p>
   * When sending {@code POST /api/rest/v1/quality-gates/{name}/calculate} with {@code includeApis}
   * referencing an API that has no stub in WireMock (i.e. the api-index-api returns 404), the
   * service must:
   * <ul>
   *   <li>return HTTP 400 Bad Request</li>
   *   <li>return a JSON error body whose {@code code} equals {@code "Bad Request"} and whose
   *       {@code message} is non-blank</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn400WhenCalculatingWithApiNotIndexed(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate("test-quality-gate")
        .getMessageBuilderSupport()
        .body(
          """
          {"includeApis":[{"serviceName":"not-indexed-service","apiName":"not-indexed-api","apiVersion":"1.0.0"}]}
          """
        )
    );

    testRunner.then(
      qualityGateApi
        .receiveCalculateQualityGate(BAD_REQUEST)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var error = JsonMapper.shared().readValue(
            payload,
            CalculateQualityGate400Response.class
          );
          assertThat(error.getCode()).isEqualTo(BAD_REQUEST.getReasonPhrase());
          assertThat(error.getMessage()).isNotBlank();
        })
    );
  }

  /**
   * Verifies that a calculate request for a non-existent quality gate results in HTTP 404.
   *
   * <p>
   * Given that the api-index-api stub resolves the requested APIs successfully, when sending
   * {@code POST /api/rest/v1/quality-gates/non-existent-quality-gate/calculate}, the service must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   *   <li>return a JSON error body whose {@code code} equals {@code "Not Found"} and whose
   *       {@code message} contains the quality gate name</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenQualityGateNotFound(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate("non-existent-quality-gate")
        .getMessageBuilderSupport()
        .body(VALID_REQUEST_BODY)
    );

    testRunner.then(
      qualityGateApi
        .receiveCalculateQualityGate(NOT_FOUND)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var error = JsonMapper.shared().readValue(
            payload,
            CalculateQualityGate400Response.class
          );
          assertThat(error.getCode()).isEqualTo(NOT_FOUND.getReasonPhrase());
          assertThat(error.getMessage()).contains("non-existent-quality-gate");
        })
    );
  }

  /**
   * Verifies that a valid calculate request is accepted and a report is returned.
   *
   * <p>
   * Given WireMock stubs for both the api-index-api and the quality-gate-api, when sending a valid
   * {@code POST /api/rest/v1/quality-gates/test-quality-gate/calculate}, the service must:
   * <ul>
   *   <li>return HTTP 202 Accepted</li>
   *   <li>return a JSON body with a non-null {@code calculationId}, {@code qualityGateConfigName}
   *       matching the request, and {@code status} equal to {@code IN_PROGRESS}</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldCalculateQualityGate(@CitrusResource TestCaseRunner testRunner) {
    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate("test-quality-gate")
        .getMessageBuilderSupport()
        .body(VALID_REQUEST_BODY)
    );

    testRunner.then(
      qualityGateApi
        .receiveCalculateQualityGate(ACCEPTED)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var report = JsonMapper.shared().readValue(
            payload,
            CalculateQualityGate202Response.class
          );
          assertThat(report.getCalculationId()).isNotNull();
          assertThat(report.getQualityGateConfigName()).isEqualTo(
            "test-quality-gate"
          );
          assertThat(report.getStatus()).isEqualTo(
            CalculateQualityGate202Response.StatusEnum.IN_PROGRESS
          );
        })
    );
  }
}

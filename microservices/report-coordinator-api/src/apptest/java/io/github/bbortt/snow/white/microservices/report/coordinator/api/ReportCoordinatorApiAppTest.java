/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.CitrusUtils.getHttpEndpoint;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.math.BigDecimal.valueOf;
import static java.time.Duration.ofSeconds;
import static org.assertj.core.api.Assertions.assertThat;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.housekeeping.api.HousekeepingApi;
import org.citrusframework.automation.qualitygate.api.QualityGateApi;
import org.citrusframework.automation.report.api.ReportApi;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.kafka.message.KafkaMessage;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Black-box reference test suite for the report-coordinator-api service.
 * <p>
 * These tests exercise the packaged application exactly as it runs in production: REST calls hit
 * the real HTTP endpoints, the "API index" and "quality-gate" dependencies are stubbed via
 * WireMock, and the asynchronous coverage calculation is simulated by publishing a response event
 * to the real Kafka response topic - exactly as openapi-coverage-stream would.
 */
@CitrusSupport
class ReportCoordinatorApiAppTest {

  private static final String KAFKA_BOOTSTRAP_SERVERS = getProperty(
    "kafka.bootstrap.servers",
    "localhost:9092"
  );

  private static final String API_DETAILS_PATH_TEMPLATE =
    "/api/rest/v1/apis/{otelServiceName}/{apiName}/{apiVersion}";
  private static final String QUALITY_GATE_CONFIG_PATH_TEMPLATE =
    "/api/rest/v1/quality-gates/{name}";

  private static QualityGateApi qualityGateApi;
  private static ReportApi reportApi;
  private static HousekeepingApi housekeepingApi;

  @BindToRegistry
  private final KafkaEndpoint openApiCoverageResponseEndpoint =
    KafkaEndpoint.builder()
      .randomConsumerGroup(true)
      .server(
        getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
      )
      .topic(
        getProperty(
          "openapi-calculation-response.topic",
          "snow-white-openapi-calculation-response"
        )
      )
      .useThreadSafeConsumer()
      .build();

  @BeforeAll
  static void beforeAllSetup() {
    var httpEndpoint = getHttpEndpoint(
      getProperty("report-coordinator-api.host", "localhost"),
      parseInt(getProperty("report-coordinator-api.port", "8084"))
    );

    qualityGateApi = new QualityGateApi(httpEndpoint);
    reportApi = new ReportApi(httpEndpoint);
    housekeepingApi = new HousekeepingApi(httpEndpoint);

    WireMock.configureFor(
      getProperty("wiremock.host", "localhost"),
      parseInt(getProperty("wiremock.port", "9000"))
    );
  }

  @BeforeEach
  void beforeEachSetup() {
    openApiCoverageResponseEndpoint
      .getEndpointConfiguration()
      .setValueSerializer(JsonSerializer.class);

    reset();
  }

  /**
   * When a quality-gate calculation is initiated for an API that is both indexed and configured,
   * the service must accept the request, persist a new report and immediately expose it as
   * {@code IN_PROGRESS} together with a {@code Location} header pointing to it.
   */
  @Test
  @CitrusTest
  void shouldInitializeQualityGateCalculation(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var serviceName = "shouldInitializeQualityGateCalculation-service";
    var apiName = "shouldInitializeQualityGateCalculation-api";
    var apiVersion = "1.0.0";
    var qualityGateConfigName = "shouldInitializeQualityGateCalculation-gate";

    stubApiIndexExists(serviceName, apiName, apiVersion);
    stubQualityGateConfig(qualityGateConfigName);

    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate(qualityGateConfigName)
        .getMessageBuilderSupport()
        .body(calculateRequestBody(serviceName, apiName, apiVersion))
    );

    testRunner.then(
      qualityGateApi
        .receiveCalculateQualityGate(ACCEPTED)
        .message()
        .validate((message, context) -> {
          assertThat((String) message.getHeader(LOCATION)).isNotBlank();

          var report = JsonMapper.shared().readValue(
            message.getPayload(String.class),
            Map.class
          );

          assertThat(report.get("qualityGateConfigName")).isEqualTo(
            qualityGateConfigName
          );
          assertThat(report.get("status")).isEqualTo("IN_PROGRESS");
          assertThat((String) report.get("calculationId")).isNotBlank();
        })
    );
  }

  /**
   * When the requested quality-gate configuration does not exist, the calculation must not be
   * started and the service must respond with HTTP 404, without ever contacting the API index.
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenQualityGateConfigDoesNotExist(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var qualityGateConfigName =
      "shouldReturn404WhenQualityGateConfigDoesNotExist-gate";

    stubFor(
      get(urlPathTemplate(QUALITY_GATE_CONFIG_PATH_TEMPLATE))
        .withPathParam("name", equalTo(qualityGateConfigName))
        .willReturn(notFound())
    );

    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate(qualityGateConfigName)
        .getMessageBuilderSupport()
        .body(
          calculateRequestBody(
            "shouldReturn404WhenQualityGateConfigDoesNotExist-service",
            "shouldReturn404WhenQualityGateConfigDoesNotExist-api",
            "1.0.0"
          )
        )
    );

    testRunner.then(
      qualityGateApi
        .receiveCalculateQualityGate(NOT_FOUND)
        .message()
        .validate((message, context) -> {
          var error = JsonMapper.shared().readValue(
            message.getPayload(String.class),
            Map.class
          );

          assertThat(error.get("code")).isEqualTo(NOT_FOUND.getReasonPhrase());
          assertThat((String) error.get("message")).contains(
            qualityGateConfigName
          );
        })
    );
  }

  /**
   * When one of the requested APIs is not known to the API index, the calculation must not be
   * started and the service must respond with HTTP 400.
   */
  @Test
  @CitrusTest
  void shouldReturn400WhenApiIsNotIndexed(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var serviceName = "shouldReturn400WhenApiIsNotIndexed-service";
    var apiName = "shouldReturn400WhenApiIsNotIndexed-api";
    var apiVersion = "1.0.0";

    stubFor(
      get(urlPathTemplate(API_DETAILS_PATH_TEMPLATE))
        .withPathParam("otelServiceName", equalTo(serviceName))
        .withPathParam("apiName", equalTo(apiName))
        .withPathParam("apiVersion", equalTo(apiVersion))
        .willReturn(notFound())
    );

    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate("shouldReturn400WhenApiIsNotIndexed-gate")
        .getMessageBuilderSupport()
        .body(calculateRequestBody(serviceName, apiName, apiVersion))
    );

    testRunner.then(
      qualityGateApi
        .receiveCalculateQualityGate(BAD_REQUEST)
        .message()
        .validate((message, context) -> {
          var error = JsonMapper.shared().readValue(
            message.getPayload(String.class),
            Map.class
          );

          assertThat(error.get("code")).isEqualTo(
            BAD_REQUEST.getReasonPhrase()
          );
          assertThat((String) error.get("message")).contains("not indexed");
        })
    );
  }

  /**
   * Verifies the full asynchronous calculation lifecycle: once the OpenAPI coverage response
   * event arrives on the real Kafka response topic - exactly as openapi-coverage-stream would
   * publish it - the report must transition from {@code IN_PROGRESS} to {@code PASSED}, and both
   * the JSON and JUnit XML representations must reflect the completed calculation.
   */
  @Test
  @CitrusTest
  void shouldCompleteFullQualityGateLifecycle(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var serviceName = "shouldCompleteFullQualityGateLifecycle-service";
    var apiName = "shouldCompleteFullQualityGateLifecycle-api";
    var apiVersion = "1.0.0";
    var qualityGateConfigName = "shouldCompleteFullQualityGateLifecycle-gate";

    stubApiIndexExists(serviceName, apiName, apiVersion);
    stubQualityGateConfig(qualityGateConfigName);

    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate(qualityGateConfigName)
        .getMessageBuilderSupport()
        .body(calculateRequestBody(serviceName, apiName, apiVersion))
    );

    var calculationIdHolder = new UUID[1];
    testRunner.then(
      qualityGateApi
        .receiveCalculateQualityGate(ACCEPTED)
        .message()
        .validate((message, context) -> {
          var report = JsonMapper.shared().readValue(
            message.getPayload(String.class),
            Map.class
          );
          calculationIdHolder[0] = UUID.fromString(
            (String) report.get("calculationId")
          );
        })
    );

    var calculationId = calculationIdHolder[0];

    var openApiCoverageResponseEvent = new OpenApiCoverageResponseEvent(
      ApiInformation.builder()
        .serviceName(serviceName)
        .apiName(apiName)
        .apiVersion(apiVersion)
        .apiType(ApiType.OPENAPI)
        .build(),
      Set.of(
        new OpenApiTestResult(
          OpenApiCoverageCriteria.PATH_COVERAGE,
          valueOf(0.8),
          ofSeconds(1)
        )
      )
    );

    testRunner.run(
      send(openApiCoverageResponseEndpoint).message(
        new KafkaMessage(openApiCoverageResponseEvent).messageKey(
          calculationId.toString()
        )
      )
    );

    testRunner.run(
      repeatOnError()
        .index("i")
        .until("i = 15")
        .autoSleep(ofSeconds(1))
        .actions(
          reportApi.sendGetReportByCalculationId(calculationId),
          reportApi
            .receiveGetReportByCalculationId(OK)
            .message()
            .validate((message, context) -> {
              var report = JsonMapper.shared().readValue(
                message.getPayload(String.class),
                Map.class
              );

              assertThat(report.get("status")).isEqualTo("PASSED");
            })
        )
    );

    testRunner.when(
      reportApi.sendGetReportByCalculationIdAsJUnit(calculationId)
    );
    testRunner.then(
      reportApi
        .receiveGetReportByCalculationIdAsJUnit(OK)
        .message()
        .validate((message, context) -> {
          var xml = message.getPayload(String.class);
          assertThat(xml)
            .contains("<testsuites")
            .contains(calculationId.toString());
        })
    );
  }

  /**
   * Verifies that reports can be listed and filtered by the API they belong to.
   */
  @Test
  @CitrusTest
  void shouldListQualityGateReports(@CitrusResource TestCaseRunner testRunner) {
    var serviceName = "shouldListQualityGateReports-service";
    var apiName = "shouldListQualityGateReports-api";
    var apiVersion = "1.0.0";
    var qualityGateConfigName = "shouldListQualityGateReports-gate";

    stubApiIndexExists(serviceName, apiName, apiVersion);
    stubQualityGateConfig(qualityGateConfigName);

    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate(qualityGateConfigName)
        .getMessageBuilderSupport()
        .body(calculateRequestBody(serviceName, apiName, apiVersion))
    );
    testRunner.then(qualityGateApi.receiveCalculateQualityGate(ACCEPTED));

    testRunner.when(
      reportApi.sendListQualityGateReports().serviceName(serviceName)
    );
    testRunner.then(
      reportApi
        .receiveListQualityGateReports(OK)
        .message()
        .validate((message, context) -> {
          var reports = JsonMapper.shared().readValue(
            message.getPayload(String.class),
            List.class
          );

          assertThat(reports).isNotEmpty();
        })
    );
  }

  /**
   * Verifies that the housekeeping job can be triggered on demand and accepts the request
   * asynchronously.
   */
  @Test
  @CitrusTest
  void shouldTriggerHousekeeping(@CitrusResource TestCaseRunner testRunner) {
    testRunner.when(housekeepingApi.sendHousekeeping());
    testRunner.then(housekeepingApi.receiveHousekeeping(ACCEPTED));
  }

  private void stubApiIndexExists(
    String serviceName,
    String apiName,
    String apiVersion
  ) {
    stubFor(
      get(urlPathTemplate(API_DETAILS_PATH_TEMPLATE))
        .withPathParam("otelServiceName", equalTo(serviceName))
        .withPathParam("apiName", equalTo(apiName))
        .withPathParam("apiVersion", equalTo(apiVersion))
        .willReturn(
          okJson(
            JsonMapper.shared().writeValueAsString(
              Map.of(
                "serviceName",
                serviceName,
                "apiName",
                apiName,
                "apiVersion",
                apiVersion,
                "sourceUrl",
                "https://example.test/openapi.yaml",
                "apiType",
                "OPENAPI"
              )
            )
          )
        )
    );
  }

  /**
   * Stubs the quality-gate config lookup with an empty criteria list, which makes every reported
   * result irrelevant to the final status, so a report deterministically resolves to PASSED as
   * soon as any result is linked to it.
   */
  private void stubQualityGateConfig(String qualityGateConfigName) {
    stubFor(
      get(urlPathTemplate(QUALITY_GATE_CONFIG_PATH_TEMPLATE))
        .withPathParam("name", equalTo(qualityGateConfigName))
        .willReturn(
          okJson(
            JsonMapper.shared().writeValueAsString(
              Map.of(
                "name",
                qualityGateConfigName,
                "minCoveragePercentage",
                80,
                "openApiCoverageCriteria",
                Set.of()
              )
            )
          )
        )
    );
  }

  private String calculateRequestBody(
    String serviceName,
    String apiName,
    String apiVersion
  ) {
    return JsonMapper.shared().writeValueAsString(
      Map.of(
        "includeApis",
        List.of(
          Map.of(
            "serviceName",
            serviceName,
            "apiName",
            apiName,
            "apiVersion",
            apiVersion
          )
        ),
        "lookbackWindow",
        "1h"
      )
    );
  }
}

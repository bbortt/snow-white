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

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
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
import org.apache.kafka.common.serialization.Serializer;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.housekeeping.api.HousekeepingApi;
import org.citrusframework.automation.qualitygate.api.QualityGateApi;
import org.citrusframework.automation.report.api.ReportApi;
import org.citrusframework.container.RepeatOnErrorUntilTrue;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.kafka.message.KafkaMessage;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.core.type.TypeReference;
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

  // JacksonJsonSerializer<T>.class erases to a raw Class<JacksonJsonSerializer>, which the
  // compiler no longer accepts as a Class<? extends Serializer<?>> since citrus 5.0.1.
  @SuppressWarnings("unchecked")
  private static final Class<? extends Serializer<?>> VALUE_SERIALIZER = (Class<
    ? extends Serializer<?>
  >) (Class<?>) JacksonJsonSerializer.class;

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
      .setValueSerializer(VALUE_SERIALIZER);

    reset();
  }

  /**
   * When a quality-gate calculation is initiated for an API that is both indexed and configured,
   * the service must accept the request, persist a new report and immediately expose it as
   * {@code IN_PROGRESS} together with a {@code Location} header pointing to it.
   */
  @Test
  @CitrusTest
  @VerifiesSw(SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING)
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
          assertThat(message.getHeader(LOCATION)).isNotNull();

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
   * When the requested APIs are indexed but the requested quality-gate configuration does not
   * exist, the calculation must not be started and the service must respond with HTTP 404.
   */
  @Test
  @CitrusTest
  @VerifiesSw(SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING)
  void shouldReturn404WhenQualityGateConfigDoesNotExist(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var serviceName =
      "shouldReturn404WhenQualityGateConfigDoesNotExist-service";
    var apiName = "shouldReturn404WhenQualityGateConfigDoesNotExist-api";
    var apiVersion = "1.0.0";
    var qualityGateConfigName =
      "shouldReturn404WhenQualityGateConfigDoesNotExist-gate";

    stubApiIndexExists(serviceName, apiName, apiVersion);
    stubFor(
      get(urlPathTemplate(QUALITY_GATE_CONFIG_PATH_TEMPLATE))
        .withPathParam("name", equalTo(qualityGateConfigName))
        .willReturn(notFound())
    );

    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate(qualityGateConfigName)
        .getMessageBuilderSupport()
        .body(calculateRequestBody(serviceName, apiName, apiVersion))
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
  @VerifiesSw(SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING)
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
   * An API the index knows, but whose type no coverage calculator consumes, must be rejected at
   * the trigger rather than dispatched: nothing would ever answer the calculation request, and the
   * caller would learn about it only once housekeeping times the report out.
   */
  @Test
  @CitrusTest
  @VerifiesSw(SwTraceables.SW_013_CALCULATION_TRIGGER_IS_ALL_OR_NOTHING)
  void shouldReturn400WhenApiTypeHasNoCoverageCalculator(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var serviceName = "shouldReturn400WhenApiTypeHasNoCalculator-service";
    var apiName = "shouldReturn400WhenApiTypeHasNoCalculator-api";
    var apiVersion = "1.0.0";

    stubApiIndexExists(serviceName, apiName, apiVersion, ApiType.ASYNCAPI);

    testRunner.when(
      qualityGateApi
        .sendCalculateQualityGate(
          "shouldReturn400WhenApiTypeHasNoCalculator-gate"
        )
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
          assertThat((String) error.get("message")).contains(
            "is indexed as 'ASYNCAPI', which no coverage calculator serves!"
          );
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
  @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
  @VerifiesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
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

              assertReportStatus(report, "PASSED");
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
            .contains(calculationId.toString())
            // The bar the whole document was judged against is the gate's own,
            // pinned onto the report when the calculation was triggered.
            .contains("minCoveragePercentage")
            .contains("value=\"80\"");
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
   * Verifies that the report listing filters by the exact API identity, orders by the requested
   * property and pages through the result.
   *
   * <p>
   * Given two reports for one API version - under two different quality-gates - and a third for
   * another version of the same API, listing by {@code serviceName}, {@code apiName} and
   * {@code apiVersion} with {@code sort=qualityGateConfigName,desc} and a page size of one must
   * return the second gate's report on page 0, the first gate's on page 1, and never the other
   * version's.
   */
  @Test
  @CitrusTest
  @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
  @VerifiesSw(SwTraceables.SW_036_SORT_VOCABULARY_IS_THE_PUBLISHED_NAMES)
  void shouldListQualityGateReportsFilteredSortedAndPaged(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var serviceName =
      "shouldListQualityGateReportsFilteredSortedAndPaged-service";
    var apiName = "shouldListQualityGateReportsFilteredSortedAndPaged-api";
    var firstGate = "shouldListQualityGateReportsFilteredSortedAndPaged-gate-a";
    var secondGate =
      "shouldListQualityGateReportsFilteredSortedAndPaged-gate-b";

    stubApiIndexExists(serviceName, apiName, "1.0.0");
    stubApiIndexExists(serviceName, apiName, "2.0.0");
    stubQualityGateConfig(firstGate);
    stubQualityGateConfig(secondGate);

    triggerCalculation(testRunner, firstGate, serviceName, apiName, "1.0.0");
    triggerCalculation(testRunner, secondGate, serviceName, apiName, "1.0.0");
    // same gate as the second report, but another version: must be filtered out
    triggerCalculation(testRunner, secondGate, serviceName, apiName, "2.0.0");

    for (var page = 0; page < 2; page++) {
      var expectedGate = page == 0 ? secondGate : firstGate;

      testRunner.when(
        reportApi
          .sendListQualityGateReports()
          .serviceName(serviceName)
          .apiName(apiName)
          .apiVersion("1.0.0")
          .sort(sortValue("qualityGateConfigName", "desc"))
          .page(page)
          .size(1)
      );
      testRunner.then(
        reportApi
          .receiveListQualityGateReports(OK)
          .message()
          .validate((message, context) -> {
            var reports = JsonMapper.shared().readValue(
              message.getPayload(String.class),
              new TypeReference<List<Map<String, Object>>>() {}
            );

            assertThat(reports)
              .extracting(report -> report.get("qualityGateConfigName"))
              .containsExactly(expectedGate);
          })
      );
    }
  }

  /**
   * Verifies that the report listing rejects a {@code sort} property it cannot order by with HTTP
   * 400.
   *
   * <p>
   * {@code status} is published on every report, but deliberately not sortable: it is stored as a
   * stable code whose order is not the order its names imply. Asking for it must answer HTTP 400
   * with an error body naming the properties the listing does accept.
   */
  @Test
  @CitrusTest
  @VerifiesSw(SwTraceables.SW_038_UNUSABLE_SORT_IS_REJECTED_NOT_IGNORED)
  void shouldReturn400WhenSortingReportsByUnsortableProperty(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      reportApi.sendListQualityGateReports().sort(sortValue("status", "asc"))
    );

    testRunner.then(
      reportApi
        .receiveListQualityGateReports(BAD_REQUEST)
        .message()
        .validate((message, context) -> {
          var error = JsonMapper.shared().readValue(
            message.getPayload(String.class),
            Map.class
          );

          assertThat(error.get("code")).isEqualTo(
            BAD_REQUEST.getReasonPhrase()
          );
          assertThat((String) error.get("message"))
            .contains("status")
            .contains("calculationId", "initiatedAt", "qualityGateConfigName");
        })
    );
  }

  /**
   * Verifies that both representations of a report answer HTTP 404 for a calculation id no report
   * carries, with an error body naming that id.
   */
  @Test
  @CitrusTest
  @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
  void shouldReturn404WhenReportDoesNotExist(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var calculationId = UUID.randomUUID();

    testRunner.when(reportApi.sendGetReportByCalculationId(calculationId));
    testRunner.then(
      reportApi
        .receiveGetReportByCalculationId(NOT_FOUND)
        .message()
        .validate((message, context) ->
          assertReportNotFound(message.getPayload(String.class), calculationId)
        )
    );

    testRunner.when(
      reportApi.sendGetReportByCalculationIdAsJUnit(calculationId)
    );
    testRunner.then(
      reportApi
        .receiveGetReportByCalculationIdAsJUnit(NOT_FOUND)
        .message()
        .validate((message, context) ->
          assertReportNotFound(message.getPayload(String.class), calculationId)
        )
    );
  }

  /**
   * Verifies that the JUnit XML representation of a report still {@code IN_PROGRESS} answers HTTP
   * 202 with the report as it currently stands, in JSON - there is no verdict to export yet.
   */
  @Test
  @CitrusTest
  @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
  void shouldReturn202WhenJUnitReportIsStillInProgress(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var serviceName = "shouldReturn202WhenJUnitReportIsStillInProgress-service";
    var apiName = "shouldReturn202WhenJUnitReportIsStillInProgress-api";
    var apiVersion = "1.0.0";
    var qualityGateConfigName =
      "shouldReturn202WhenJUnitReportIsStillInProgress-gate";

    stubApiIndexExists(serviceName, apiName, apiVersion);
    stubQualityGateConfig(qualityGateConfigName);

    // No OpenApiCoverageResponseEvent is ever published, so the report stays IN_PROGRESS.
    var calculationId = triggerCalculation(
      testRunner,
      qualityGateConfigName,
      serviceName,
      apiName,
      apiVersion
    );

    testRunner.when(
      reportApi.sendGetReportByCalculationIdAsJUnit(calculationId)
    );
    testRunner.then(
      reportApi
        .receiveGetReportByCalculationIdAsJUnit(ACCEPTED)
        .message()
        .validate((message, context) -> {
          var report = JsonMapper.shared().readValue(
            message.getPayload(String.class),
            Map.class
          );

          assertThat(report.get("calculationId")).isEqualTo(
            calculationId.toString()
          );
          assertThat(report.get("status")).isEqualTo("IN_PROGRESS");
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

  /**
   * A report whose OpenApiCoverageResponseEvent never arrives - e.g. openapi-coverage-stream
   * crashed, or the message was lost - would otherwise stay {@code IN_PROGRESS} forever.
   * Housekeeping must resolve it to a terminal {@code TIMED_OUT} state once its cutoff has
   * elapsed, rather than leaving it stuck with no observable outcome.
   */
  @Test
  @CitrusTest
  @VerifiesSw(SwTraceables.SW_018_STALE_REPORTS_TIME_OUT_NOT_DELETED)
  void shouldTimeOutStaleReport(@CitrusResource TestCaseRunner testRunner) {
    var serviceName = "shouldTimeOutStaleReport-service";
    var apiName = "shouldTimeOutStaleReport-api";
    var apiVersion = "1.0.0";
    var qualityGateConfigName = "shouldTimeOutStaleReport-gate";

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

    // The report is deliberately left IN_PROGRESS - no OpenApiCoverageResponseEvent is ever published -
    // to simulate openapi-coverage-stream never responding.
    // Triggering housekeeping repeatedly is harmless before the cutoff elapses,
    // so retrying the whole trigger-then-check cycle doubles as the wait for the (apptest-shortened) cutoff window.
    testRunner.run(
      repeatOnError()
        .index("i")
        .until("i = 15")
        .autoSleep(ofSeconds(1))
        .actions(
          housekeepingApi.sendHousekeeping(),
          housekeepingApi.receiveHousekeeping(ACCEPTED),
          reportApi.sendGetReportByCalculationId(calculationId),
          reportApi
            .receiveGetReportByCalculationId(OK)
            .message()
            .validate((message, context) -> {
              var report = JsonMapper.shared().readValue(
                message.getPayload(String.class),
                Map.class
              );

              assertReportStatus(report, "TIMED_OUT");
            })
        )
    );
  }

  /**
   * {@link RepeatOnErrorUntilTrue} only retries on {@link CitrusRuntimeException} -
   * a plain AssertJ {@link AssertionError} from inside a {@code validate()} callback propagates straight out on the first attempt instead of triggering a retry.
   */
  private void assertReportStatus(Map<?, ?> report, String expectedStatus) {
    var actualStatus = report.get("status");
    if (!expectedStatus.equals(actualStatus)) {
      throw new CitrusRuntimeException(
        "Expected report status '" +
          expectedStatus +
          "' but was '" +
          actualStatus +
          "'"
      );
    }
  }

  /**
   * Triggers a calculation for one API and answers the calculation id the report was created
   * under.
   */
  private UUID triggerCalculation(
    TestCaseRunner testRunner,
    String qualityGateConfigName,
    String serviceName,
    String apiName,
    String apiVersion
  ) {
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

    return calculationIdHolder[0];
  }

  private static void assertReportNotFound(String payload, UUID calculationId) {
    var error = JsonMapper.shared().readValue(payload, Map.class);

    assertThat(error.get("code")).isEqualTo(NOT_FOUND.getReasonPhrase());
    assertThat((String) error.get("message")).contains(
      calculationId.toString()
    );
  }

  /**
   * A {@code property,direction} sort value as Citrus has to be handed it. Citrus joins a message's
   * query parameters with {@code ,} and splits them apart again when building the URL, so a literal
   * comma would cut the value short - the server would receive just {@code property}. The comma is
   * written as {@code %2C} instead, which Citrus decodes back to {@code ,} in the URL it sends.
   */
  private static String sortValue(String property, String direction) {
    return property + "%2C" + direction;
  }

  private void stubApiIndexExists(
    String serviceName,
    String apiName,
    String apiVersion
  ) {
    stubApiIndexExists(serviceName, apiName, apiVersion, ApiType.OPENAPI);
  }

  private void stubApiIndexExists(
    String serviceName,
    String apiName,
    String apiVersion,
    ApiType apiType
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
                apiType.name()
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

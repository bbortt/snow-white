/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.OtlpTraceFixtures.Span.span;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.container.Assert.Builder.assertException;
import static org.citrusframework.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static org.citrusframework.kafka.endpoint.KafkaMessageFilter.kafkaMessageFilter;
import static org.citrusframework.kafka.endpoint.selector.KafkaMessageSelectorFactory.KafkaMessageSelectorFactories.factoryWithKafkaMessageSelector;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization.OpenApiCoverageResponseEventJsonDeserializer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.kafka.endpoint.selector.KafkaMessageSelector;
import org.citrusframework.kafka.message.KafkaMessage;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Shared black-box scenarios for openapi-coverage-stream, run once per telemetry backend
 * (see {@link OpenApiCoverageStreamInfluxDbAppTest} / {@link OpenApiCoverageStreamGrafanaAppTest}).
 * <p>
 * Both backend instances always consume every message on the shared {@code snow-white-calculation-request} topic -
 * only the response topic differs per instance ({@link #responseTopic()}) -
 * so running these scenarios against each subclass proves the two telemetry backends are interchangeable for every branch,
 * not just the identical-results happy path (see {@link OpenApiCoverageStreamCrossBackendAppTest} for that direct comparison).
 */
@CitrusSupport
abstract class AbstractOpenApiCoverageStreamAppTest {

  private static final String KAFKA_BOOTSTRAP_SERVERS = getProperty(
    "kafka.bootstrap.servers",
    "localhost:9092"
  );

  private static final String API_DETAILS_PATH_TEMPLATE =
    "/api/rest/v1/apis/{otelServiceName}/{apiName}/{apiVersion}";
  private static final String VALID_SPEC_PATH = "/openapi/pets-api.yaml";
  private static final String WIREMOCK_INTERNAL_BASE_URL =
    "http://wiremock:8080";

  private static final String VALID_SPEC_CONTENT = readClasspathResource(
    "openapi/pets-api.yaml"
  );

  /**
   * The collector's groupbytrace processor holds spans open for 5s before batching them onward to InfluxDB/Tempo (see {@code src/apptest/resources/otel-collector/config.yaml}) -
   * telemetry published onto snow-white_outbound is only reliably queryable by either backend after this margin.
   */
  private static final Duration TELEMETRY_INGESTION_MARGIN = Duration.ofSeconds(
    35L
  );

  private static final Duration RESPONSE_LOOKBACK_WINDOW = Duration.ofSeconds(
    30L
  );

  @BindToRegistry
  private final KafkaEndpoint calculationRequestEndpoint =
    KafkaEndpoint.builder()
      .randomConsumerGroup(true)
      .server(
        getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
      )
      .topic(
        getProperty(
          "calculation-request.topic",
          "snow-white-calculation-request"
        )
      )
      .useThreadSafeConsumer()
      .build();

  @BindToRegistry
  private final KafkaEndpoint otlpTraceEndpoint = KafkaEndpoint.builder()
    .randomConsumerGroup(true)
    .server(
      getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
    )
    .topic(getProperty("otlp-trace.topic", "snow-white_outbound"))
    .useThreadSafeConsumer()
    .build();

  @BindToRegistry
  private final KafkaEndpoint openApiCoverageResponseEndpoint =
    KafkaEndpoint.builder()
      .randomConsumerGroup(true)
      .server(
        getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
      )
      .topic(responseTopic())
      .useThreadSafeConsumer()
      .build();

  protected abstract String responseTopic();

  @BeforeAll
  static void beforeAllSetup() {
    WireMock.configureFor(
      getProperty("wiremock.host", "localhost"),
      parseInt(getProperty("wiremock.port", "9000"))
    );

    TelemetryPipelineWarmup.ensureWarm(
      getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS),
      getProperty("otlp-trace.topic", "snow-white_outbound")
    );
  }

  @BeforeEach
  void beforeEachSetup() {
    calculationRequestEndpoint
      .getEndpointConfiguration()
      .setValueSerializer(JacksonJsonSerializer.class);

    openApiCoverageResponseEndpoint
      .getEndpointConfiguration()
      .setValueDeserializer(OpenApiCoverageResponseEventJsonDeserializer.class);
    openApiCoverageResponseEndpoint
      .getEndpointConfiguration()
      .getKafkaMessageSelectorFactory()
      .setCustomStrategies(
        factoryWithKafkaMessageSelector(
          selectors ->
            selectors.containsKey(
              KafkaMessageByKeySelector.MESSAGE_KEY_FILTER_KEY
            ),
          selectors ->
            new KafkaMessageByKeySelector(
              (String) selectors.get(
                KafkaMessageByKeySelector.MESSAGE_KEY_FILTER_KEY
              )
            )
        )
      );

    reset();
  }

  /**
   * The straightforward pass-through case: telemetry matching every documented path is available,
   * the API is indexed and its specification parses cleanly, so a full coverage result -
   * one entry per {@link OpenApiCoverageCriteria} -
   * is returned without an error.
   */
  @Test
  @CitrusTest
  void shouldReturnFullCoverage_whenTelemetryDataMatches(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "happy-path-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";

    stubApiIndexed(serviceName, apiName, apiVersion, VALID_SPEC_PATH);

    publishTelemetry(
      runner,
      span(serviceName, "GET /pets")
        .attribute(HTTP_REQUEST_METHOD.getKey(), "GET")
        .attribute(URL_PATH.getKey(), "/pets")
        .attribute(HTTP_RESPONSE_STATUS_CODE.getKey(), "200")
        .attribute("api.name", apiName)
        .attribute("api.version", apiVersion),
      span(serviceName, "GET /pets/{petId}")
        .attribute(HTTP_REQUEST_METHOD.getKey(), "GET")
        .attribute(URL_PATH.getKey(), "/pets/{petId}")
        .attribute(HTTP_RESPONSE_STATUS_CODE.getKey(), "200")
        .attribute("api.name", apiName)
        .attribute("api.version", apiVersion)
    );

    var response = sendCalculationRequestAndAwaitResponse(
      runner,
      serviceName,
      apiName,
      apiVersion,
      null
    );

    assertThat(response.errorMessage()).isNull();
    assertThat(response.openApiTestResults())
      .extracting("openApiCriteria")
      .containsExactlyInAnyOrder((Object[]) OpenApiCoverageCriteria.values());

    var pathCoverage = response
      .openApiTestResults()
      .stream()
      .filter(
        result ->
          result.openApiCriteria() == OpenApiCoverageCriteria.PATH_COVERAGE
      )
      .findFirst()
      .orElseThrow();
    assertThat(pathCoverage.coverage()).isEqualByComparingTo("1.00");
  }

  /**
   * When the telemetry backend has no data matching the request's criteria at all,
   * the calculation short-circuits with a descriptive error instead of returning an empty/zeroed result set.
   */
  @Test
  @CitrusTest
  void shouldReturnError_whenNoTelemetryDataFound(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "no-telemetry-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";

    stubApiIndexed(serviceName, apiName, apiVersion, VALID_SPEC_PATH);

    var response = sendCalculationRequestAndAwaitResponse(
      runner,
      serviceName,
      apiName,
      apiVersion,
      null
    );

    assertThat(response.openApiTestResults()).isNull();
    assertThat(response.errorMessage()).startsWith(
      "Did not find any telemetry data with configured criteria"
    );
  }

  /**
   * Attribute filters on the request narrow which telemetry counts towards coverage - here,
   * only one of two documented paths carries the requested attribute value,
   * so path coverage must reflect exactly that one path, not both.
   */
  @Test
  @CitrusTest
  void shouldApplyAttributeFilters_whenNarrowingTelemetryData(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "attribute-filter-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";

    stubApiIndexed(serviceName, apiName, apiVersion, VALID_SPEC_PATH);

    publishTelemetry(
      runner,
      span(serviceName, "GET /pets")
        .attribute(HTTP_REQUEST_METHOD.getKey(), "GET")
        .attribute(URL_PATH.getKey(), "/pets")
        .attribute(HTTP_RESPONSE_STATUS_CODE.getKey(), "200")
        .attribute("api.name", apiName)
        .attribute("api.version", apiVersion)
        .attribute("environment", "prod"),
      span(serviceName, "GET /pets/{petId}")
        .attribute(HTTP_REQUEST_METHOD.getKey(), "GET")
        .attribute(URL_PATH.getKey(), "/pets/{petId}")
        .attribute(HTTP_RESPONSE_STATUS_CODE.getKey(), "200")
        .attribute("api.name", apiName)
        .attribute("api.version", apiVersion)
        .attribute("environment", "staging")
    );

    var response = sendCalculationRequestAndAwaitResponse(
      runner,
      serviceName,
      apiName,
      apiVersion,
      Set.of(new AttributeFilter("environment", STRING_EQUALS, "prod"))
    );

    assertThat(response.errorMessage()).isNull();
    var pathCoverage = response
      .openApiTestResults()
      .stream()
      .filter(
        result ->
          result.openApiCriteria() == OpenApiCoverageCriteria.PATH_COVERAGE
      )
      .findFirst()
      .orElseThrow();
    assertThat(pathCoverage.coverage()).isEqualByComparingTo("0.50");
  }

  /**
   * When the API is not known to api-index-api,
   * the calculation must fail fast with a descriptive error instead of attempting to query telemetry at all.
   */
  @Test
  @CitrusTest
  void shouldReturnError_whenApiIsNotIndexed(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "not-indexed-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";

    stubFor(
      get(urlPathTemplate(API_DETAILS_PATH_TEMPLATE))
        .withPathParam("otelServiceName", equalTo(serviceName))
        .withPathParam("apiName", equalTo(apiName))
        .withPathParam("apiVersion", equalTo(apiVersion))
        .willReturn(notFound())
    );

    var response = sendCalculationRequestAndAwaitResponse(
      runner,
      serviceName,
      apiName,
      apiVersion,
      null
    );

    assertThat(response.openApiTestResults()).isNull();
    assertThat(response.errorMessage()).contains(
      "OpenApi identifier not indexed"
    );
  }

  /**
   * When the API is indexed but its source document does not parse as OpenAPI,
   * the calculation must fail fast with a descriptive error.
   */
  @Test
  @CitrusTest
  void shouldReturnError_whenOpenApiSpecIsUnparseable(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "unparseable-spec-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";
    var specPath = "/openapi/invalid.yaml";

    stubApiIndexed(serviceName, apiName, apiVersion, specPath);
    stubFor(
      get(urlEqualTo(specPath)).willReturn(
        aResponse().withBody("not-a-valid-openapi-document")
      )
    );

    var response = sendCalculationRequestAndAwaitResponse(
      runner,
      serviceName,
      apiName,
      apiVersion,
      null
    );

    assertThat(response.openApiTestResults()).isNull();
    assertThat(response.errorMessage()).contains("Unparsable OpenAPI");
  }

  /**
   * A single transient api-index-api failure must be recovered from automatically by the built-in retry,
   * so the calculation still completes successfully.
   */
  @Test
  @CitrusTest
  void shouldRecover_whenApiIndexFailsTransiently(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "transient-failure-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";
    var scenarioName = "transient-api-index-failure";
    var recoveredState = "recovered";

    // Both backend instances always consume every request on the shared topic, so this
    // scenario is hit concurrently by two independent callers. WireMock's scenario-state
    // transition isn't guaranteed atomic under that race - a request can land in a moment
    // where the STARTED stub no longer matches but the state hasn't visibly become
    // "recovered" yet either. Rather than gate recovery on a second scenario-state stub
    // (which would leave that gap unmatched -> a spurious 404), the fallback below is
    // unconditional and lower-priority: only the very first, cleanly-STARTED hit can ever
    // see a failure, and every other request - regardless of scenario state - succeeds.
    stubFor(
      get(urlPathTemplate(API_DETAILS_PATH_TEMPLATE))
        .withPathParam("otelServiceName", equalTo(serviceName))
        .withPathParam("apiName", equalTo(apiName))
        .withPathParam("apiVersion", equalTo(apiVersion))
        .inScenario(scenarioName)
        .whenScenarioStateIs(Scenario.STARTED)
        .willSetStateTo(recoveredState)
        .atPriority(1)
        .willReturn(serverError())
    );
    stubFor(
      get(urlPathTemplate(API_DETAILS_PATH_TEMPLATE))
        .withPathParam("otelServiceName", equalTo(serviceName))
        .withPathParam("apiName", equalTo(apiName))
        .withPathParam("apiVersion", equalTo(apiVersion))
        .atPriority(2)
        .willReturn(
          okJson(
            apiDetailsBody(serviceName, apiName, apiVersion, VALID_SPEC_PATH)
          )
        )
    );
    stubFor(
      get(urlEqualTo(VALID_SPEC_PATH)).willReturn(ok(VALID_SPEC_CONTENT))
    );

    publishTelemetry(
      runner,
      span(serviceName, "GET /pets")
        .attribute(HTTP_REQUEST_METHOD.getKey(), "GET")
        .attribute(URL_PATH.getKey(), "/pets")
        .attribute(HTTP_RESPONSE_STATUS_CODE.getKey(), "200")
        .attribute("api.name", apiName)
        .attribute("api.version", apiVersion)
    );

    var response = sendCalculationRequestAndAwaitResponse(
      runner,
      serviceName,
      apiName,
      apiVersion,
      null
    );

    assertThat(response.errorMessage()).isNull();
    assertThat(response.openApiTestResults()).isNotEmpty();
  }

  /**
   * When api-index-api is persistently unavailable,
   * the built-in retry must exhaust and the calculation must still complete with a descriptive error instead of hanging or crashing the stream.
   */
  @Test
  @CitrusTest
  void shouldReturnError_whenApiIndexIsPersistentlyUnavailable(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "persistent-failure-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";

    stubFor(
      get(urlPathTemplate(API_DETAILS_PATH_TEMPLATE))
        .withPathParam("otelServiceName", equalTo(serviceName))
        .withPathParam("apiName", equalTo(apiName))
        .withPathParam("apiVersion", equalTo(apiVersion))
        .willReturn(serverError())
    );

    var response = sendCalculationRequestAndAwaitResponse(
      runner,
      serviceName,
      apiName,
      apiVersion,
      null
    );

    assertThat(response.openApiTestResults()).isNull();
    assertThat(response.errorMessage()).contains("500");
  }

  /**
   * A request for an API type other than OpenAPI must never produce a response on this service's output topic at all -
   * it is filtered out upstream of any backend-specific processing.
   */
  @Test
  @CitrusTest
  void shouldNotRespond_whenApiTypeIsNotOpenApi(
    @CitrusResource TestActionRunner runner
  ) {
    var messageKey = randomUUID().toString();
    var event = QualityGateCalculationRequestEvent.builder()
      .apiInformation(
        ApiInformation.builder()
          .serviceName("non-openapi-service")
          .apiName("pets-api")
          .apiVersion("1.0.0")
          .apiType(ApiType.ASYNCAPI)
          .build()
      )
      .lookbackWindow("5m")
      .build();

    runner.run(
      send(calculationRequestEndpoint).message(
        new KafkaMessage(event).messageKey(messageKey)
      )
    );

    runner.run(
      assertException()
        .exception(CitrusRuntimeException.class)
        .message("@startsWith(Failed to resolve Kafka message using selector)@")
        .action(
          receive(openApiCoverageResponseEndpoint).selector(
            kafkaMessageFilter()
              .eventLookbackWindow(RESPONSE_LOOKBACK_WINDOW)
              .kafkaMessageSelector(new KafkaMessageByKeySelector(messageKey))
              .build()
          )
        )
    );
  }

  /**
   * Telemetry that does not carry the OpenAPI operation-id attribute is only routed by its literal (concrete) URL path,
   * which never matches a templated spec path - it must be dropped from coverage entirely,
   * leaving path coverage at 0%. Telemetry that does carry a known operation-id must instead be grouped under that operation's templated path,
   * regardless of what its literal URL looked like - proving the fast-path routing end-to-end.
   */
  @Test
  @CitrusTest
  void shouldGroupTelemetryByOperationId_whenPresentAndKnown(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "operation-id-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";

    stubApiIndexed(serviceName, apiName, apiVersion, VALID_SPEC_PATH);

    publishTelemetry(
      runner,
      span(serviceName, "GET /pets/123")
        .attribute(HTTP_REQUEST_METHOD.getKey(), "GET")
        .attribute(URL_PATH.getKey(), "/pets/123")
        .attribute(HTTP_RESPONSE_STATUS_CODE.getKey(), "200")
        .attribute("api.name", apiName)
        .attribute("api.version", apiVersion)
        .attribute("openapi.operation.id", "getPetById")
    );

    var response = sendCalculationRequestAndAwaitResponse(
      runner,
      serviceName,
      apiName,
      apiVersion,
      null
    );

    assertThat(response.errorMessage()).isNull();
    var pathCoverage = response
      .openApiTestResults()
      .stream()
      .filter(
        result ->
          result.openApiCriteria() == OpenApiCoverageCriteria.PATH_COVERAGE
      )
      .findFirst()
      .orElseThrow();
    assertThat(pathCoverage.coverage()).isEqualByComparingTo("0.50");
  }

  private OpenApiCoverageResponseEvent sendCalculationRequestAndAwaitResponse(
    TestActionRunner runner,
    String serviceName,
    String apiName,
    String apiVersion,
    Set<AttributeFilter> attributeFilters
  ) {
    var messageKey = randomUUID().toString();
    var event = QualityGateCalculationRequestEvent.builder()
      .apiInformation(
        ApiInformation.builder()
          .serviceName(serviceName)
          .apiName(apiName)
          .apiVersion(apiVersion)
          .apiType(ApiType.OPENAPI)
          .build()
      )
      .lookbackWindow("5m")
      .attributeFilters(attributeFilters)
      .build();

    runner.run(
      send(calculationRequestEndpoint).message(
        new KafkaMessage(event).messageKey(messageKey)
      )
    );

    var responseHolder = new OpenApiCoverageResponseEvent[1];
    runner.run(
      repeatOnError()
        .index("i")
        .until("i = 10")
        .autoSleep(Duration.ofSeconds(2))
        .actions(
          receive(openApiCoverageResponseEndpoint)
            .selector(
              kafkaMessageFilter()
                .eventLookbackWindow(RESPONSE_LOOKBACK_WINDOW)
                .kafkaMessageSelector(new KafkaMessageByKeySelector(messageKey))
                .build()
            )
            .message()
            .validate(
              (message, context) ->
                responseHolder[0] = message.getPayload(
                  OpenApiCoverageResponseEvent.class
                )
            )
        )
    );

    return responseHolder[0];
  }

  private void publishTelemetry(
    TestActionRunner runner,
    OtlpTraceFixtures.Span... spans
  ) {
    var payload = OtlpTraceFixtures.traceRequestJson(spans);

    runner.run(
      send(otlpTraceEndpoint).message(
        new KafkaMessage(payload).messageKey(randomUUID().toString())
      )
    );

    awaitTelemetryIngestion();
  }

  private void awaitTelemetryIngestion() {
    try {
      Thread.sleep(TELEMETRY_INGESTION_MARGIN.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(
        "Interrupted while awaiting telemetry ingestion",
        e
      );
    }
  }

  private void stubApiIndexed(
    String serviceName,
    String apiName,
    String apiVersion,
    String specPath
  ) {
    stubFor(
      get(urlPathTemplate(API_DETAILS_PATH_TEMPLATE))
        .withPathParam("otelServiceName", equalTo(serviceName))
        .withPathParam("apiName", equalTo(apiName))
        .withPathParam("apiVersion", equalTo(apiVersion))
        .willReturn(
          okJson(apiDetailsBody(serviceName, apiName, apiVersion, specPath))
        )
    );
    stubFor(get(urlEqualTo(specPath)).willReturn(ok(VALID_SPEC_CONTENT)));
  }

  private String apiDetailsBody(
    String serviceName,
    String apiName,
    String apiVersion,
    String specPath
  ) {
    return JsonMapper.shared().writeValueAsString(
      Map.of(
        "serviceName",
        serviceName,
        "apiName",
        apiName,
        "apiVersion",
        apiVersion,
        "sourceUrl",
        WIREMOCK_INTERNAL_BASE_URL + specPath,
        "apiType",
        "OPENAPI"
      )
    );
  }

  private static String readClasspathResource(String path) {
    try (
      InputStream inputStream = AbstractOpenApiCoverageStreamAppTest.class
        .getClassLoader()
        .getResourceAsStream(path)
    ) {
      if (inputStream == null) {
        throw new IllegalStateException(
          "Classpath resource not found: " + path
        );
      }
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  record KafkaMessageByKeySelector(
    String key
  ) implements KafkaMessageSelector<String> {
    static final String MESSAGE_KEY_FILTER_KEY = "message-key";

    @Override
    public boolean matches(ConsumerRecord<Object, Object> consumerRecord) {
      return consumerRecord.key() != null && consumerRecord.key().equals(key);
    }

    @Override
    public Map<String, String> asSelector() {
      return Map.of(MESSAGE_KEY_FILTER_KEY, key);
    }
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.OtlpTraceFixtures.Span.span;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static org.citrusframework.kafka.endpoint.KafkaMessageFilter.kafkaMessageFilter;
import static org.citrusframework.kafka.endpoint.selector.KafkaMessageSelectorFactory.KafkaMessageSelectorFactories.factoryWithKafkaMessageSelector;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization.OpenApiCoverageResponseEventJsonDeserializer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
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
 * Both telemetry backend instances always consume every message on the shared
 * {@code snow-white-calculation-request} topic and the collector persists every trace to both
 * InfluxDB and Grafana Tempo (see src/apptest/resources/otel-collector/config.yaml) - so a single
 * request must yield identical coverage results on both response topics, proving the two backends
 * are genuinely interchangeable rather than merely both individually correct (see
 * {@link AbstractOpenApiCoverageStreamAppTest} for the per-backend scenario coverage).
 */
@CitrusSupport
class OpenApiCoverageStreamCrossBackendAppTest {

  private static final String KAFKA_BOOTSTRAP_SERVERS = getProperty(
    "kafka.bootstrap.servers",
    "localhost:9092"
  );

  private static final String API_DETAILS_PATH_TEMPLATE =
    "/api/rest/v1/apis/{otelServiceName}/{apiName}/{apiVersion}";
  private static final String SPEC_PATH = "/openapi/pets-api.yaml";
  private static final String WIREMOCK_INTERNAL_BASE_URL =
    "http://wiremock:8080";

  private static final String SPEC_CONTENT = readClasspathResource(
    "openapi/pets-api.yaml"
  );

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
  private final KafkaEndpoint influxDbResponseEndpoint = KafkaEndpoint.builder()
    .randomConsumerGroup(true)
    .server(
      getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
    )
    .topic(
      getProperty(
        "openapi-calculation-response-influxdb.topic",
        "snow-white-openapi-calculation-response-influxdb"
      )
    )
    .useThreadSafeConsumer()
    .build();

  @BindToRegistry
  private final KafkaEndpoint grafanaResponseEndpoint = KafkaEndpoint.builder()
    .randomConsumerGroup(true)
    .server(
      getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
    )
    .topic(
      getProperty(
        "openapi-calculation-response-grafana.topic",
        "snow-white-openapi-calculation-response-grafana"
      )
    )
    .useThreadSafeConsumer()
    .build();

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

    for (var endpoint : new KafkaEndpoint[] {
      influxDbResponseEndpoint,
      grafanaResponseEndpoint,
    }) {
      endpoint
        .getEndpointConfiguration()
        .setValueDeserializer(
          OpenApiCoverageResponseEventJsonDeserializer.class
        );
      endpoint
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
    }

    reset();
  }

  @Test
  @CitrusTest
  void shouldReturnIdenticalCoverage_forBothTelemetryBackends(
    @CitrusResource TestActionRunner runner
  ) {
    var serviceName = "cross-backend-service";
    var apiName = "pets-api";
    var apiVersion = "1.0.0";

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
                WIREMOCK_INTERNAL_BASE_URL + SPEC_PATH,
                "apiType",
                "OPENAPI"
              )
            )
          )
        )
    );
    stubFor(get(urlEqualTo(SPEC_PATH)).willReturn(ok(SPEC_CONTENT)));

    var payload = OtlpTraceFixtures.traceRequestJson(
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
    runner.run(
      send(otlpTraceEndpoint).message(
        new KafkaMessage(payload).messageKey(randomUUID().toString())
      )
    );
    awaitTelemetryIngestion();

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
      .build();

    runner.run(
      send(calculationRequestEndpoint).message(
        new KafkaMessage(event).messageKey(messageKey)
      )
    );

    var influxDbResponse = new OpenApiCoverageResponseEvent[1];
    runner.run(
      repeatOnError()
        .index("i")
        .until("i = 10")
        .autoSleep(Duration.ofSeconds(2))
        .actions(
          receive(influxDbResponseEndpoint)
            .selector(
              kafkaMessageFilter()
                .eventLookbackWindow(RESPONSE_LOOKBACK_WINDOW)
                .kafkaMessageSelector(new KafkaMessageByKeySelector(messageKey))
                .build()
            )
            .message()
            .validate(
              (message, context) ->
                influxDbResponse[0] = message.getPayload(
                  OpenApiCoverageResponseEvent.class
                )
            )
        )
    );

    var grafanaResponse = new OpenApiCoverageResponseEvent[1];
    runner.run(
      repeatOnError()
        .index("i")
        .until("i = 10")
        .autoSleep(Duration.ofSeconds(2))
        .actions(
          receive(grafanaResponseEndpoint)
            .selector(
              kafkaMessageFilter()
                .eventLookbackWindow(RESPONSE_LOOKBACK_WINDOW)
                .kafkaMessageSelector(new KafkaMessageByKeySelector(messageKey))
                .build()
            )
            .message()
            .validate(
              (message, context) ->
                grafanaResponse[0] = message.getPayload(
                  OpenApiCoverageResponseEvent.class
                )
            )
        )
    );

    assertThat(influxDbResponse[0].errorMessage()).isNull();
    assertThat(grafanaResponse[0].errorMessage()).isNull();

    assertThat(coverageByCriteria(influxDbResponse[0])).isEqualTo(
      coverageByCriteria(grafanaResponse[0])
    );
  }

  private Map<OpenApiCoverageCriteria, BigDecimal> coverageByCriteria(
    OpenApiCoverageResponseEvent response
  ) {
    return response
      .openApiTestResults()
      .stream()
      .collect(
        toMap(result -> result.openApiCriteria(), result -> result.coverage())
      );
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

  private static String readClasspathResource(String path) {
    try (
      InputStream inputStream = OpenApiCoverageStreamCrossBackendAppTest.class
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

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.OtelEventFilterStreamAppTest.KafkaMessageByKeySelector.MESSAGE_KEY_FILTER_KEY;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.API_NAME;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.API_VERSION;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.OTEL_SERVICE_NAME;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.TRACE_ID;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.TestData.wrapResourceSpans;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.INBOUND_TOPIC_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.OUTBOUND_TOPIC_PROPERTY_NAME;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.Objects.nonNull;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.container.Assert.Builder.assertException;
import static org.citrusframework.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static org.citrusframework.kafka.endpoint.KafkaMessageFilter.kafkaMessageFilter;
import static org.citrusframework.kafka.endpoint.selector.KafkaMessageSelectorFactory.KafkaMessageSelectorFactories.factoryWithKafkaMessageSelector;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.serialization.ExportTraceServiceRequestJsonDeserializer;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.serialization.ExportTraceServiceRequestJsonSerializer;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.exceptions.MessageTimeoutException;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.kafka.endpoint.selector.KafkaMessageSelector;
import org.citrusframework.kafka.message.KafkaMessage;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Black-box reference test suite for the otel-event-filter-stream service.
 * <p>
 * These tests exercise the packaged application exactly as it runs in production: events are
 * sent through the real inbound Kafka topic, the real "API index" dependency is stubbed via
 * WireMock, and assertions are made against the real outbound Kafka topic. They serve as a
 * behavioral reference when rewriting this microservice.
 */
@CitrusSupport
class OtelEventFilterStreamAppTest {

  private static final String KAFKA_BOOTSTRAP_SERVERS = getProperty(
    "kafka.bootstrap.servers",
    "localhost:9092"
  );

  private static final Duration PASS_THROUGH_LOOKBACK_WINDOW =
    Duration.ofSeconds(20L);
  private static final Duration DROP_LOOKBACK_WINDOW = Duration.ofSeconds(10L);
  private static final long DROP_ASSERTION_TIMEOUT_MS = 6000L;
  private static final long RETRY_EXHAUSTED_DROP_ASSERTION_TIMEOUT_MS = 8000L;

  private static final String EXISTS_PATH_TEMPLATE =
    "/api/rest/v1/apis/{serviceName}/{apiName}/{apiVersion}/exists";

  private final TestData testData = TestData.builder().build();

  @BindToRegistry
  private final KafkaEndpoint inboundEndpoint = KafkaEndpoint.builder()
    .randomConsumerGroup(true)
    .server(
      getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
    )
    .topic(getProperty(INBOUND_TOPIC_PROPERTY_NAME, "snow-white_inbound"))
    .useThreadSafeConsumer()
    .build();

  @BindToRegistry
  private final KafkaEndpoint outboundEndpoint = KafkaEndpoint.builder()
    .randomConsumerGroup(true)
    .server(
      getProperty("spring.kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
    )
    .topic(getProperty(OUTBOUND_TOPIC_PROPERTY_NAME, "snow-white_outbound"))
    .useThreadSafeConsumer()
    .build();

  @BeforeAll
  static void beforeAllSetup() {
    WireMock.configureFor(
      getProperty("wiremock.host", "localhost"),
      parseInt(getProperty("wiremock.port", "9000"))
    );
  }

  @BeforeEach
  void beforeEachSetup() {
    inboundEndpoint
      .getEndpointConfiguration()
      .setValueSerializer(ExportTraceServiceRequestJsonSerializer.class);
    outboundEndpoint
      .getEndpointConfiguration()
      .setValueDeserializer(ExportTraceServiceRequestJsonDeserializer.class);

    outboundEndpoint
      .getEndpointConfiguration()
      .getKafkaMessageSelectorFactory()
      .setCustomStrategies(
        factoryWithKafkaMessageSelector(
          messageSelectors ->
            messageSelectors.containsKey(MESSAGE_KEY_FILTER_KEY),
          messageSelectors ->
            new KafkaMessageByKeySelector(
              (String) messageSelectors.get(MESSAGE_KEY_FILTER_KEY)
            )
        )
      );

    reset();
  }

  /**
   * The service shall filter inbound Kafka events based on message information:
   * <ol>
   *     <li>API Name</li>
   *     <li>API Version</li>
   *     <li>OTEL Service Name</li>
   * </ol>
   * Only if the API exists within the snow-white system, the event must be routed through.
   * Otherwise, it shall be dropped.
   * <p>
   * When an inbound event with information of an existing API is received, the service must send it to the outbound queue exactly as received.
   */
  @Test
  @CitrusTest
  void shouldPassThroughMatchingInboundEvent(
    @CitrusResource TestActionRunner runner
  ) {
    stubApiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

    sendAndExpectPassThrough(
      runner,
      TRACE_ID,
      wrapResourceSpans(testData.resourceSpansWithResourceAttributes())
    );
  }

  /**
   * The API-identifying attributes are looked up on the resource level first, but fall back to
   * the instrumentation scope when they are absent there. An event carrying its API identity on
   * the scope level must therefore also be routed through unmodified.
   */
  @Test
  @CitrusTest
  void shouldPassThroughMatchingInboundEventWithApiIdentifiersOnScopeLevel(
    @CitrusResource TestActionRunner runner
  ) {
    stubApiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

    sendAndExpectPassThrough(
      runner,
      "scope-level-api-identifiers",
      wrapResourceSpans(testData.resourceSpansWithScopeAttributes())
    );
  }

  /**
   * As a last resort, the API-identifying attributes are looked up on the individual span level.
   * An event carrying its API identity there must also be routed through unmodified.
   */
  @Test
  @CitrusTest
  void shouldPassThroughMatchingInboundEventWithApiIdentifiersOnSpanLevel(
    @CitrusResource TestActionRunner runner
  ) {
    stubApiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

    sendAndExpectPassThrough(
      runner,
      "span-level-api-identifiers",
      wrapResourceSpans(testData.resourceSpansWithSpanAttributes())
    );
  }

  /**
   * The three API-identifying attributes do not need to originate from the same level - they may
   * be spread across resource, scope and span attributes. As long as all three can be resolved,
   * the event must be routed through unmodified.
   */
  @Test
  @CitrusTest
  void shouldPassThroughMatchingInboundEventWithApiIdentifiersAcrossAllLevels(
    @CitrusResource TestActionRunner runner
  ) {
    stubApiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

    sendAndExpectPassThrough(
      runner,
      "mixed-level-api-identifiers",
      wrapResourceSpans(testData.resourceSpansWithAttributesOnEachLevel())
    );
  }

  /**
   * When the API referenced by an inbound event is not known to snow-white - i.e. the API index
   * reports it does not exist - the event must be dropped and never reach the outbound topic.
   */
  @Test
  @CitrusTest
  void shouldDropInboundEventOfUnknownApi(
    @CitrusResource TestActionRunner runner
  ) {
    stubFor(
      get(urlPathTemplate(EXISTS_PATH_TEMPLATE))
        .withPathParam("serviceName", equalTo(OTEL_SERVICE_NAME))
        .withPathParam("apiName", equalTo(API_NAME))
        .withPathParam("apiVersion", equalTo(API_VERSION))
        .willReturn(notFound())
    );

    sendAndExpectDrop(
      runner,
      "unknown-api",
      wrapResourceSpans(testData.resourceSpansWithResourceAttributes()),
      DROP_ASSERTION_TIMEOUT_MS
    );

    verify(
      1,
      getRequestedFor(urlPathTemplate(EXISTS_PATH_TEMPLATE))
        .withPathParam("serviceName", equalTo(OTEL_SERVICE_NAME))
        .withPathParam("apiName", equalTo(API_NAME))
        .withPathParam("apiVersion", equalTo(API_VERSION))
    );
  }

  /**
   * When an inbound event is missing one of the three API-identifying attributes on every level,
   * the API cannot be identified at all. Such an event must be dropped without ever calling the
   * API index.
   */
  @Test
  @CitrusTest
  void shouldDropInboundEventMissingApiIdentifyingAttributes(
    @CitrusResource TestActionRunner runner
  ) {
    sendAndExpectDrop(
      runner,
      "missing-api-identifiers",
      wrapResourceSpans(testData.resourceSpansWithoutApiVersion()),
      DROP_ASSERTION_TIMEOUT_MS
    );

    verify(0, getRequestedFor(urlPathMatching("/api/rest/v1/apis/.*")));
  }

  /**
   * An inbound event that resolves to a known API but does not carry any spans at all must still
   * be dropped, since there is no telemetry left to forward once filtering is applied.
   */
  @Test
  @CitrusTest
  void shouldDropInboundEventWithoutAnySpans(
    @CitrusResource TestActionRunner runner
  ) {
    stubApiExists(OTEL_SERVICE_NAME, API_NAME, API_VERSION);

    sendAndExpectDrop(
      runner,
      "without-any-spans",
      wrapResourceSpans(testData.resourceSpansWithoutScopeSpans()),
      DROP_ASSERTION_TIMEOUT_MS
    );
  }

  /**
   * The API index lookup is retried on server errors. When the API index is persistently
   * unavailable, the service must exhaust its retries, recover gracefully and drop the event
   * instead of failing the whole stream.
   */
  @Test
  @CitrusTest
  void shouldDropInboundEventWhenApiIndexServiceIsPersistentlyUnavailable(
    @CitrusResource TestActionRunner runner
  ) {
    stubFor(
      get(urlPathTemplate(EXISTS_PATH_TEMPLATE))
        .withPathParam("serviceName", equalTo(OTEL_SERVICE_NAME))
        .withPathParam("apiName", equalTo(API_NAME))
        .withPathParam("apiVersion", equalTo(API_VERSION))
        .willReturn(serverError())
    );

    sendAndExpectDrop(
      runner,
      "persistently-unavailable-api-index",
      wrapResourceSpans(testData.resourceSpansWithResourceAttributes()),
      RETRY_EXHAUSTED_DROP_ASSERTION_TIMEOUT_MS
    );

    verify(
      3,
      getRequestedFor(urlPathTemplate(EXISTS_PATH_TEMPLATE))
        .withPathParam("serviceName", equalTo(OTEL_SERVICE_NAME))
        .withPathParam("apiName", equalTo(API_NAME))
        .withPathParam("apiVersion", equalTo(API_VERSION))
    );
  }

  /**
   * When the API index only fails transiently - e.g. a single server error - the built-in retry
   * must recover automatically and the event must still be routed through once the API index
   * responds successfully.
   */
  @Test
  @CitrusTest
  void shouldPassThroughInboundEventAfterTransientApiIndexServiceFailure(
    @CitrusResource TestActionRunner runner
  ) {
    var scenarioName = "transient-api-index-failure";
    var recoveredState = "recovered";

    stubFor(
      get(urlPathTemplate(EXISTS_PATH_TEMPLATE))
        .withPathParam("serviceName", equalTo(OTEL_SERVICE_NAME))
        .withPathParam("apiName", equalTo(API_NAME))
        .withPathParam("apiVersion", equalTo(API_VERSION))
        .inScenario(scenarioName)
        .whenScenarioStateIs(Scenario.STARTED)
        .willSetStateTo(recoveredState)
        .willReturn(serverError())
    );
    stubFor(
      get(urlPathTemplate(EXISTS_PATH_TEMPLATE))
        .withPathParam("serviceName", equalTo(OTEL_SERVICE_NAME))
        .withPathParam("apiName", equalTo(API_NAME))
        .withPathParam("apiVersion", equalTo(API_VERSION))
        .inScenario(scenarioName)
        .whenScenarioStateIs(recoveredState)
        .willReturn(ok())
    );

    sendAndExpectPassThrough(
      runner,
      "transient-api-index-failure",
      wrapResourceSpans(testData.resourceSpansWithResourceAttributes())
    );
  }

  private void stubApiExists(
    String serviceName,
    String apiName,
    String apiVersion
  ) {
    stubFor(
      get(urlPathTemplate(EXISTS_PATH_TEMPLATE))
        .withPathParam("serviceName", equalTo(serviceName))
        .withPathParam("apiName", equalTo(apiName))
        .withPathParam("apiVersion", equalTo(apiVersion))
        .willReturn(ok())
    );
  }

  private void sendAndExpectPassThrough(
    TestActionRunner runner,
    String messageKey,
    ExportTraceServiceRequest exportTraceServiceRequest
  ) {
    runner.run(
      send(inboundEndpoint).message(
        new KafkaMessage(exportTraceServiceRequest).messageKey(messageKey)
      )
    );

    runner.run(
      repeatOnError()
        .index("i")
        .until("i = 10")
        .autoSleep(Duration.ofSeconds(2))
        .actions(
          receive(outboundEndpoint).selector(
            kafkaMessageFilter()
              .eventLookbackWindow(PASS_THROUGH_LOOKBACK_WINDOW)
              .kafkaMessageSelector(new KafkaMessageByKeySelector(messageKey))
              .build()
          )
        )
    );
  }

  private void sendAndExpectDrop(
    TestActionRunner runner,
    String messageKey,
    ExportTraceServiceRequest exportTraceServiceRequest,
    long timeoutMs
  ) {
    runner.run(
      send(inboundEndpoint).message(
        new KafkaMessage(exportTraceServiceRequest).messageKey(messageKey)
      )
    );

    runner.run(
      assertException()
        .exception(MessageTimeoutException.class)
        .action(
          receive(outboundEndpoint)
            .timeout(timeoutMs)
            .selector(
              kafkaMessageFilter()
                .eventLookbackWindow(DROP_LOOKBACK_WINDOW)
                .kafkaMessageSelector(new KafkaMessageByKeySelector(messageKey))
                .build()
            )
        )
    );
  }

  record KafkaMessageByKeySelector(
    String key
  ) implements KafkaMessageSelector<String> {
    static final String MESSAGE_KEY_FILTER_KEY = "message-key";

    @Override
    public boolean matches(ConsumerRecord<Object, Object> consumerRecord) {
      return nonNull(consumerRecord.key()) && consumerRecord.key().equals(key);
    }

    @Override
    public Map<String, String> asSelector() {
      return Map.of(MESSAGE_KEY_FILTER_KEY, key);
    }
  }
}

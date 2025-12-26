/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
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
import static org.citrusframework.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static org.citrusframework.kafka.endpoint.KafkaMessageFilter.kafkaMessageFilter;
import static org.citrusframework.kafka.endpoint.selector.KafkaMessageSelectorFactory.KafkaMessageSelectorFactories.factoryWithKafkaMessageSelector;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.serialization.ExportTraceServiceRequestJsonDeserializer;
import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka.serialization.ExportTraceServiceRequestJsonSerializer;
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

@CitrusSupport
class OtelEventFilterStreamAppTest {

  private static final String KAFKA_BOOTSTRAP_SERVERS = getProperty(
    "kafka.bootstrap.servers",
    "localhost:9092"
  );

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
    stubFor(
      get(
        urlPathTemplate(
          "/api/rest/v1/apis/{serviceName}/{apiName}/{apiVersion}/exists"
        )
      )
        .withPathParam("serviceName", equalTo(OTEL_SERVICE_NAME))
        .withPathParam("apiName", equalTo(API_NAME))
        .withPathParam("apiVersion", equalTo(API_VERSION))
        .willReturn(ok())
    );

    runner.run(
      send(inboundEndpoint).message(
        new KafkaMessage(
          wrapResourceSpans(testData.resourceSpansWithResourceAttributes())
        ).messageKey(TRACE_ID)
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
              .eventLookbackWindow(Duration.ofSeconds(20L))
              .kafkaMessageSelector(new KafkaMessageByKeySelector(TRACE_ID))
              .build()
          )
        )
    );
  }

  record KafkaMessageByKeySelector(String key) implements
    KafkaMessageSelector<String> {
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

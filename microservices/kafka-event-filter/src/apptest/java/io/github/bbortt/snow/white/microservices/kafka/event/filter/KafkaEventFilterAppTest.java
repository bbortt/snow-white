/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter;

import static io.github.bbortt.snow.white.commons.redis.RedisHashUtils.generateRedisApiInformationId;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.KafkaEventFilterAppTest.KafkaMessageByKeySelector.MESSAGE_KEY_FILTER_KEY;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.API_NAME;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.API_VERSION;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.OTEL_SERVICE_NAME;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.TRACE_ID;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.wrapResourceSpans;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.INBOUND_TOPIC_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.OUTBOUND_TOPIC_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.service.impl.redis.RedisCachingService.HASH_PREFIX;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.Objects.nonNull;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static org.citrusframework.kafka.endpoint.KafkaMessageFilter.kafkaMessageFilter;
import static org.citrusframework.kafka.endpoint.selector.KafkaMessageSelectorFactory.KafkaMessageSelectorFactories.factoryWithKafkaMessageSelector;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.serialization.ExportTraceServiceRequestJsonDeserializer;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.serialization.ExportTraceServiceRequestJsonSerializer;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

@CitrusSupport
class KafkaEventFilterAppTest {

  private static final String KAFKA_BOOTSTRAP_SERVERS = getProperty(
    "kafka.bootstrap.servers",
    "localhost:9092"
  );

  private static final String REDIS_HOST = getProperty(
    "redis.host",
    "localhost"
  );
  private static final int REDIS_PORT = parseInt(
    getProperty("redis.port", "6379")
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

  private static void writeApiInformationToRedis() {
    try (var jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
      jedis.set(
        HASH_PREFIX +
          generateRedisApiInformationId(
            OTEL_SERVICE_NAME,
            API_NAME,
            API_VERSION
          ),
        "exists"
      );
    }
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

  @Test
  @CitrusTest
  void shouldPassThroughMatchingInboundEvent(
    @CitrusResource TestActionRunner runner
  ) {
    writeApiInformationToRedis();

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

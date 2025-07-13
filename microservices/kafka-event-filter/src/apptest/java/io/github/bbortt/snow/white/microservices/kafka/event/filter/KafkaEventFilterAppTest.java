/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.KafkaEventFilterAppTest.KafkaMessageByKeySelector.MESSAGE_KEY_FILTER_KEY;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.RESOURCE_SPANS_WITH_RESOURCE_ATTRIBUTES;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.TRACE_ID;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.TestData.wrapResourceSpans;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.INBOUND_TOPIC_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.OUTBOUND_TOPIC_PROPERTY_NAME;
import static java.lang.System.getProperty;
import static java.util.Objects.nonNull;
import static org.citrusframework.actions.ReceiveMessageAction.Builder.receive;
import static org.citrusframework.actions.SendMessageAction.Builder.send;
import static org.citrusframework.container.RepeatOnErrorUntilTrue.Builder.repeatOnError;
import static org.citrusframework.kafka.endpoint.KafkaMessageFilter.kafkaMessageFilter;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.serialization.ExportTraceServiceRequestJsonDeserializer;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.serialization.ExportTraceServiceRequestJsonSerializer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.citrusframework.TestActionRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.citrusframework.kafka.endpoint.KafkaEndpoint;
import org.citrusframework.kafka.endpoint.KafkaMessageFilter;
import org.citrusframework.kafka.endpoint.selector.KafkaMessageSelector;
import org.citrusframework.kafka.endpoint.selector.KafkaMessageSelectorFactory;
import org.citrusframework.kafka.message.KafkaMessage;
import org.citrusframework.spi.BindToRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@CitrusSupport
class KafkaEventFilterAppTest {

  private KafkaMessageFilter.KafkaMessageFilterBuilder getKafkaMessageFilter() {
    var messageFilterBuilder = kafkaMessageFilter();
    setField(
      messageFilterBuilder,
      "kafkaMessageSelectorFactory",
      new CustomKafkaSelectorFactory(),
      KafkaMessageSelectorFactory.class
    );
    return messageFilterBuilder;
  }

  @BindToRegistry
  private final KafkaEndpoint inboundEndpoint = KafkaEndpoint.builder()
    .randomConsumerGroup(true)
    .server(getProperty("spring.kafka.bootstrap.servers", "localhost:9094"))
    .topic(getProperty(INBOUND_TOPIC_PROPERTY_NAME, "snow-white_inbound"))
    .useThreadSafeConsumer()
    .build();

  @BindToRegistry
  private final KafkaEndpoint outboundEndpoint = KafkaEndpoint.builder()
    .randomConsumerGroup(true)
    .server(getProperty("spring.kafka.bootstrap.servers", "localhost:9094"))
    .topic(getProperty(OUTBOUND_TOPIC_PROPERTY_NAME, "snow-white_outbound"))
    .useThreadSafeConsumer()
    .build();

  @BeforeEach
  void beforeEachSetup() {
    inboundEndpoint
      .getEndpointConfiguration()
      .setValueSerializer(ExportTraceServiceRequestJsonSerializer.class);
    outboundEndpoint
      .getEndpointConfiguration()
      .setValueDeserializer(ExportTraceServiceRequestJsonDeserializer.class);
  }

  @Test
  @CitrusTest
  public void shouldPassThroughMatchingInboundEvent(
    @CitrusResource TestActionRunner runner
  ) {
    runner.run(
      send(inboundEndpoint).message(
        new KafkaMessage(
          wrapResourceSpans(RESOURCE_SPANS_WITH_RESOURCE_ATTRIBUTES)
        ).messageKey(TRACE_ID)
      )
    );

    runner.run(
      repeatOnError()
        .actions(
          receive(outboundEndpoint).selector(
            getKafkaMessageFilter()
              .eventLookbackWindow(Duration.ofMinutes(1L))
              .kafkaMessageSelector(new KafkaMessageByKeySelector(TRACE_ID))
              .build()
          )
        )
        .timeout(Duration.ofMinutes(1))
    );
  }

  record KafkaMessageByKeySelector(String key) implements KafkaMessageSelector {
    static final String MESSAGE_KEY_FILTER_KEY = "message-key";

    @Override
    public boolean matches(ConsumerRecord<Object, Object> consumerRecord) {
      return nonNull(consumerRecord.key()) && consumerRecord.key().equals(key);
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public Map<String, String> asSelector() {
      return Map.of(MESSAGE_KEY_FILTER_KEY, key);
    }
  }

  private static class CustomKafkaSelectorFactory
    extends KafkaMessageSelectorFactory {

    private static final Map<
      Predicate<Map<String, Object>>,
      Function<Map<String, Object>, KafkaMessageSelector>
    > customStrategies = new HashMap<>();

    static {
      customStrategies.put(
        messageSelectors ->
          messageSelectors.containsKey(MESSAGE_KEY_FILTER_KEY),
        messageSelectors ->
          new KafkaMessageByKeySelector(
            (String) messageSelectors.get(MESSAGE_KEY_FILTER_KEY)
          )
      );
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public <T> KafkaMessageSelector parseFromSelector(
      Map<String, T> messageSelectors
    ) {
      return customStrategies
        .entrySet()
        .stream()
        .filter(strategy ->
          strategy.getKey().test((Map<String, Object>) messageSelectors)
        )
        .findFirst()
        .map(Map.Entry::getValue)
        .map(supplier -> supplier.apply((Map<String, Object>) messageSelectors))
        .orElseGet(() -> super.parseFromSelector(messageSelectors));
    }
  }
}

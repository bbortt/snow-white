/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KafkaTopicManagerTest {

  public static final String TEST_INBOUND_TOPIC =
    "KafkaTopicManagerTest:inbound";
  public static final String TEST_OUTBOUND_TOPIC =
    "KafkaTopicManagerTest:outbound";

  private KafkaTopicManager fixture;

  @BeforeEach
  void beforeEachSetup() {
    var kafkaEventFilterProperties = new KafkaEventFilterProperties();
    kafkaEventFilterProperties.setInboundTopicName(TEST_INBOUND_TOPIC);
    kafkaEventFilterProperties.setOutboundTopicName(TEST_OUTBOUND_TOPIC);

    fixture = new KafkaTopicManager(kafkaEventFilterProperties);
  }

  @Test
  void shouldBeEnabled_whenPropertyIsSet() {
    var contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(KafkaTopicManager.class);

    var kafkaEventFilterPropertiesMock = mock(KafkaEventFilterProperties.class);

    contextRunner
      .withBean(KafkaEventFilterProperties.class, () ->
        kafkaEventFilterPropertiesMock
      )
      .withPropertyValues(PREFIX + ".init-topics=true")
      .run(context ->
        assertThat(context)
          .asInstanceOf(type(AssertableApplicationContext.class))
          .satisfies(
            c -> assertThat(c).hasSingleBean(KafkaTopicManager.class),
            c ->
              assertThat(c).getBean("inboundTopic", NewTopic.class).isNotNull(),
            c ->
              assertThat(c).getBean("outboundTopic", NewTopic.class).isNotNull()
          )
      );

    verify(kafkaEventFilterPropertiesMock).getInboundTopicName();
    verify(kafkaEventFilterPropertiesMock).getOutboundTopicName();
  }

  @Test
  void shouldNotBeEnabled_ifPropertyIsNotSet() {
    var contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(KafkaTopicManager.class);

    contextRunner.run(context ->
      assertThat(context).doesNotHaveBean(KafkaTopicManager.class)
    );
  }

  @Test
  void testInboundTopicCreation() {
    NewTopic inboundTopic = fixture.inboundTopic();

    assertThat(inboundTopic.name()).isEqualTo(TEST_INBOUND_TOPIC);
  }

  @Test
  void testOutboundTopicCreation() {
    NewTopic outboundTopic = fixture.outboundTopic();

    assertThat(outboundTopic.name()).isEqualTo(TEST_OUTBOUND_TOPIC);
  }
}

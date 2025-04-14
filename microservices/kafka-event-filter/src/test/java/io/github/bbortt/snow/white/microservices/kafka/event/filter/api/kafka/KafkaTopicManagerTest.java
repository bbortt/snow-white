/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

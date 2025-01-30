package io.github.bbortt.snow.white.kafka.microservices.event.filter.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.kafka.microservices.event.filter.config.KafkaEventFilterProperties;
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
    var inboundTopic = fixture.inboundTopic();

    assertThat(inboundTopic.name()).isEqualTo(TEST_INBOUND_TOPIC);
  }

  @Test
  void testOutboundTopicCreation() {
    var outboundTopic = fixture.outboundTopic();

    assertThat(outboundTopic.name()).isEqualTo(TEST_OUTBOUND_TOPIC);
  }
}

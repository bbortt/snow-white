package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.INBOUND_TOPIC_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.OUTBOUND_TOPIC_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@IntegrationTest
@SpringBootTest(
  properties = {
    "io.github.bbortt.snow.white.kafka.event.filter.schema-registry-url=mock://KafkaEventFilterPropertiesIT",
  }
)
class KafkaEventFilterPropertiesIT {

  @Value("${" + INBOUND_TOPIC_PROPERTY_NAME + "}")
  private String inboundTopic;

  @Value("${" + OUTBOUND_TOPIC_PROPERTY_NAME + "}")
  private String outboundTopic;

  @Autowired
  private KafkaEventFilterProperties kafkaEventFilterProperties;

  @Test
  void inboundTopicNameAndPropertyNameMatch() {
    assertThat(inboundTopic).isEqualTo(
      kafkaEventFilterProperties.getInboundTopicName()
    );
  }

  @Test
  void outboundTopicNameAndPropertyNameMatch() {
    assertThat(outboundTopic).isEqualTo(
      kafkaEventFilterProperties.getOutboundTopicName()
    );
  }
}

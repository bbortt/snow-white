package io.github.bbortt.snow.white.kafka.microservices.event.filter.kafka;

import static io.github.bbortt.snow.white.kafka.microservices.event.filter.config.KafkaEventFilterProperties.PREFIX;

import io.github.bbortt.snow.white.kafka.microservices.event.filter.config.KafkaEventFilterProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(
  prefix = PREFIX,
  value = "init-topics",
  havingValue = "true"
)
public class KafkaTopicManager {

  private final KafkaEventFilterProperties kafkaEventFilterProperties;

  public KafkaTopicManager(
    KafkaEventFilterProperties kafkaEventFilterProperties
  ) {
    this.kafkaEventFilterProperties = kafkaEventFilterProperties;
  }

  @Bean
  public NewTopic inboundTopic() {
    return TopicBuilder.name(
      kafkaEventFilterProperties.getInboundTopicName()
    ).build();
  }

  @Bean
  public NewTopic outboundTopic() {
    return TopicBuilder.name(
      kafkaEventFilterProperties.getOutboundTopicName()
    ).build();
  }
}

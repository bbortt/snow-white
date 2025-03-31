package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class KafkaTopicManager {

  private final KafkaEventFilterProperties kafkaEventFilterProperties;

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

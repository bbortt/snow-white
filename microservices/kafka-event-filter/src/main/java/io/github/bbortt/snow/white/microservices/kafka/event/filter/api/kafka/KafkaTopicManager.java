/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Slf4j
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
    logger.info("Creating topics...");

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

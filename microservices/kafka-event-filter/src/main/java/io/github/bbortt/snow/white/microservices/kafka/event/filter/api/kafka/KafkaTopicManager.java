/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.PREFIX;
import static java.lang.Boolean.FALSE;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.TopicBuilder;

@Slf4j
@Configuration
public class KafkaTopicManager {

  private final Environment environment;
  private final KafkaEventFilterProperties kafkaEventFilterProperties;

  public KafkaTopicManager(
    Environment environment,
    KafkaEventFilterProperties kafkaEventFilterProperties
  ) {
    this.environment = environment;
    this.kafkaEventFilterProperties = kafkaEventFilterProperties;
  }

  @Bean
  public NewTopic inboundTopic() {
    var initTopics = environment.getProperty(
      PREFIX + ".init-topics",
      Boolean.class,
      FALSE
    );
    if (!initTopics) {
      return null;
    }

    var inboundTopicName = kafkaEventFilterProperties.getInboundTopicName();

    logger.info("Creating inbound topic '{}'...", inboundTopicName);

    return TopicBuilder.name(inboundTopicName).build();
  }

  @Bean
  public NewTopic outboundTopic() {
    var initTopics = environment.getProperty(
      PREFIX + ".init-topics",
      Boolean.class,
      FALSE
    );
    if (!initTopics) {
      return null;
    }

    var outboundTopicName = kafkaEventFilterProperties.getOutboundTopicName();

    logger.info("Creating outbound topic '{}'...", outboundTopicName);

    return TopicBuilder.name(outboundTopicName).build();
  }
}

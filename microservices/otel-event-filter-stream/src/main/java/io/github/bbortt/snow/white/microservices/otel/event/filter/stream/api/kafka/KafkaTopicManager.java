/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.api.kafka;

import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.PREFIX;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties;
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
  private final OtelEventFilterStreamProperties otelEventFilterStreamProperties;

  public KafkaTopicManager(
    Environment environment,
    OtelEventFilterStreamProperties otelEventFilterStreamProperties
  ) {
    this.environment = environment;
    this.otelEventFilterStreamProperties = otelEventFilterStreamProperties;
  }

  @Bean
  public NewTopic inboundTopic() {
    var initTopics = environment.getProperty(
      PREFIX + ".init-topics",
      Boolean.class,
      FALSE
    );

    if (!TRUE.equals(initTopics)) {
      return null;
    }

    var inboundTopicName =
      otelEventFilterStreamProperties.getInboundTopicName();

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

    if (!TRUE.equals(initTopics)) {
      return null;
    }

    var outboundTopicName =
      otelEventFilterStreamProperties.getOutboundTopicName();

    logger.info("Creating outbound topic '{}'...", outboundTopicName);

    return TopicBuilder.name(outboundTopicName).build();
  }
}

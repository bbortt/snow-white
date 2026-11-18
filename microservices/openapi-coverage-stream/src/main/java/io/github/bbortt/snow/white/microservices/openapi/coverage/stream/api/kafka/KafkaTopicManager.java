/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka;

import static io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent.MAX_SERIALIZED_BYTES;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
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

  private final OpenApiCoverageStreamProperties openApiCoverageStreamProperties;

  public KafkaTopicManager(
    OpenApiCoverageStreamProperties openApiCoverageStreamProperties
  ) {
    logger.info("Creating topics...");

    this.openApiCoverageStreamProperties = openApiCoverageStreamProperties;
  }

  @Bean
  public NewTopic calculationRequestTopic() {
    return TopicBuilder.name(
      openApiCoverageStreamProperties.getCalculationRequestTopic()
    ).build();
  }

  /**
   * Created admitting
   * {@link io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent#MAX_SERIALIZED_BYTES},
   * because this
   * service's producer is sized by the same constant and a topic that holds less would reject what
   * the producer is allowed to send.
   * <p>
   * {@code report-coordinator-api} declares the same topic with the same limit, because either
   * service may be the one that reaches an empty cluster first.
   * <p>
   * A cluster that already holds the topic needs the limit applied rather than declared, which is
   * what {@code spring.kafka.admin.modify-topic-configs} is set for - without it this changes
   * nothing for an existing deployment, and the rejection merely moves from the producer to the
   * broker. Applying it needs {@code ALTER_CONFIGS} on the topic; where that is denied,
   * {@code KafkaAdmin} logs the failure and starts anyway, and the limit has to be set out of band.
   */
  @Bean
  public NewTopic openapiCalculationResponseTopic() {
    return TopicBuilder.name(
      openApiCoverageStreamProperties.getOpenapiCalculationResponseTopic()
    )
      .config(
        TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
        String.valueOf(MAX_SERIALIZED_BYTES)
      )
      .build();
  }
}

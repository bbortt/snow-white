/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.OpenApiCoverageStreamProperties;
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

  @Bean
  public NewTopic openapiCalculationResponseTopic() {
    return TopicBuilder.name(
      openApiCoverageStreamProperties.getOpenapiCalculationResponseTopic()
    ).build();
  }
}

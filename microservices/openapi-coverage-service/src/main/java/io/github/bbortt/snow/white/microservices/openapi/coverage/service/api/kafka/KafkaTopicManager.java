/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties;
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

  private final OpenApiCoverageServiceProperties openApiCoverageServiceProperties;

  public KafkaTopicManager(
    OpenApiCoverageServiceProperties openApiCoverageServiceProperties
  ) {
    logger.info("Creating topics...");

    this.openApiCoverageServiceProperties = openApiCoverageServiceProperties;
  }

  @Bean
  public NewTopic calculationRequestTopic() {
    return TopicBuilder.name(
      openApiCoverageServiceProperties.getCalculationRequestTopic()
    ).build();
  }

  @Bean
  public NewTopic openapiCalculationResponseTopic() {
    return TopicBuilder.name(
      openApiCoverageServiceProperties.getOpenapiCalculationResponseTopic()
    ).build();
  }
}

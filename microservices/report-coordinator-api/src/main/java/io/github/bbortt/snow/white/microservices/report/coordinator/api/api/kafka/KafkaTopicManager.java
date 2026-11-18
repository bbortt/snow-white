/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka;

import static io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent.MAX_SERIALIZED_BYTES;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Slf4j
@Configuration
public class KafkaTopicManager {

  private final ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  public KafkaTopicManager(
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    this.reportCoordinationServiceProperties =
      reportCoordinationServiceProperties;
  }

  @Bean
  public NewTopic calculationRequestTopic() {
    if (!reportCoordinationServiceProperties.isInitTopics()) {
      return null;
    }

    var calculationRequestTopic =
      reportCoordinationServiceProperties.getCalculationRequestTopic();

    logger.info(
      "Creating calculation request topic '{}'...",
      calculationRequestTopic
    );

    return TopicBuilder.name(calculationRequestTopic).build();
  }

  /**
   * Created admitting
   * {@link io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent#MAX_SERIALIZED_BYTES},
   * matching the declaration in {@code openapi-coverage-stream}.
   * <p>
   * This service only consumes the topic, so the limit is not its own concern - but it may be the
   * one that reaches an empty cluster first, and a topic created here without the limit would
   * leave the producer unable to publish into it.
   */
  @Bean
  public NewTopic openapiCalculationResponseTopic() {
    if (!reportCoordinationServiceProperties.isInitTopics()) {
      return null;
    }

    var calculationResponseTopic = reportCoordinationServiceProperties
      .getOpenapiCalculationResponse()
      .getTopic();

    logger.info(
      "Creating calculation response topic '{}'...",
      calculationResponseTopic
    );

    return TopicBuilder.name(calculationResponseTopic)
      .config(
        TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
        String.valueOf(MAX_SERIALIZED_BYTES)
      )
      .build();
  }
}

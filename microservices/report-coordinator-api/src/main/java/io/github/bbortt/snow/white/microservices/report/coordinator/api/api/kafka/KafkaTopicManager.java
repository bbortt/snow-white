/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
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

    return TopicBuilder.name(calculationResponseTopic).build();
  }
}

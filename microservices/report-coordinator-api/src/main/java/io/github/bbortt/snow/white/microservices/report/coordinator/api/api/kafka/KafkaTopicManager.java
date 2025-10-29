/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties.PREFIX;
import static java.lang.Boolean.FALSE;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
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
  private final ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  public KafkaTopicManager(
    Environment environment,
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    this.environment = environment;
    this.reportCoordinationServiceProperties =
      reportCoordinationServiceProperties;
  }

  @Bean
  public NewTopic calculationRequestTopic() {
    var initTopics = environment.getProperty(
      PREFIX + ".init-topics",
      Boolean.class,
      FALSE
    );

    if (!initTopics) {
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
    var initTopics = environment.getProperty(
      PREFIX + ".init-topics",
      Boolean.class,
      FALSE
    );

    if (!initTopics) {
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

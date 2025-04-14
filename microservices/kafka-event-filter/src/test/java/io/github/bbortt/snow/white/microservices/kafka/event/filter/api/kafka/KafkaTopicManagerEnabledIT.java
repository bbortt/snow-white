/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@Isolated
@DirtiesContext
@IntegrationTest
@TestPropertySource(
  properties = "io.github.bbortt.snow.white.kafka.event.filter.init-topics=true"
)
class KafkaTopicManagerEnabledIT {

  @Autowired
  private KafkaTopicManager kafkaTopicManager;

  @Test
  void isEnabledUsingProperties() {
    assertThat(kafkaTopicManager).isNotNull();
  }
}

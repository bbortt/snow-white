/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(
  properties = {
    "io.github.bbortt.snow.white.microservices.report.coordination.service.init-topics=false",
  }
)
class KafkaTopicManagerIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void isNotInitializedPerDefault() {
    assertThatThrownBy(() -> applicationContext.getBean(KafkaTopicManager.class)
    ).isInstanceOf(NoSuchBeanDefinitionException.class);
  }
}

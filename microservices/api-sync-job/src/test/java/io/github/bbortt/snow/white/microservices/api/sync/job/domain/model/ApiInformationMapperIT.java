/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@IntegrationTest
class ApiInformationMapperIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void isRegisteredWithinSpringComponentModel() {
    assertThat(
      applicationContext.getBean(ApiInformationMapper.class)
    ).isNotNull();
  }
}

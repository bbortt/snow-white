/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.sync.job.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class ApiSyncJobPropertiesValidatorIT {

  @Autowired
  private ApiSyncJobPropertiesValidator apiSyncJobPropertiesValidator;

  @Test
  void shouldBeRegisteredWithinSpringContext() {
    assertThat(apiSyncJobPropertiesValidator).isNotNull();
  }
}

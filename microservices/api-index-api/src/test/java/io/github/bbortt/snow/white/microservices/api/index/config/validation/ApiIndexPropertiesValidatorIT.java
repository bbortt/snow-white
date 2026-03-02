/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.index.AbstractApiIndexApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiIndexPropertiesValidatorIT extends AbstractApiIndexApiIT {

  @Autowired
  private ApiIndexPropertiesValidator apiIndexPropertiesValidator;

  @Test
  void shouldBeRegisteredWithinSpringContext() {
    assertThat(apiIndexPropertiesValidator).isNotNull();
  }
}

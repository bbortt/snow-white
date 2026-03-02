/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InfluxDBPropertiesValidatorIT extends AbstractOpenApiCoverageServiceIT {

  @Autowired
  private InfluxDBPropertiesValidator influxDBPropertiesValidator;

  @Test
  void shouldBeRegisteredWithinSpringContext() {
    assertThat(influxDBPropertiesValidator).isNotNull();
  }
}

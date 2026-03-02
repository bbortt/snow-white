/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(
  properties = {
    "snow.white.otel.event.filter.api-index.base-url=http://localhost:8085",
  }
)
class OtelEventFilterStreamPropertiesValidatorIT {

  @Autowired
  private OtelEventFilterStreamPropertiesValidator otelEventFilterStreamPropertiesValidator;

  @Test
  void shouldBeRegisteredWithinSpringContext() {
    assertThat(otelEventFilterStreamPropertiesValidator).isNotNull();
  }
}

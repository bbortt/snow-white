/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenTelemetryConfigUnitTest {

  private OpenTelemetryConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenTelemetryConfig();
  }

  @Nested
  class OpenTelemetryTest {

    @Test
    void returnsOpenTelemetryTest() {
      assertThat(fixture.openTelemetry())
        .isNotNull()
        .isInstanceOf(OpenTelemetry.class);
    }
  }
}

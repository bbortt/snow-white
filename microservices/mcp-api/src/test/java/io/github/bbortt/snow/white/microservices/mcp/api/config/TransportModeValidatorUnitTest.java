/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static io.github.bbortt.snow.white.microservices.mcp.api.config.TransportModeValidator.STDIO_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.MapPropertySource;

/**
 * The other side of {@code TransportModeIT}: a stdio run is the one case where the
 * {@code stdio} property being true is correct. That run starts without a web context, and
 * in-process is the only place to assert it — the launched process of
 * {@code StdioTransportIT} cannot be observed from here.
 */
class TransportModeValidatorUnitTest {

  @Test
  void aStdioRunHasNoWebContextAndSoNothingToReject() {
    var applicationContext = new StaticApplicationContext();
    applicationContext
      .getEnvironment()
      .getPropertySources()
      .addFirst(
        new MapPropertySource("test", Map.of(STDIO_PROPERTY_NAME, "true"))
      );

    assertThatNoException().isThrownBy(() ->
      new TransportModeValidator(applicationContext).afterPropertiesSet()
    );
  }
}

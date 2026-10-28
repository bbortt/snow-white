/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static io.github.bbortt.snow.white.microservices.mcp.api.config.TransportModeValidator.STDIO_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * The other side of {@code TransportModeIT}: a stdio run is the one case where the
 * {@code stdio} property being true is correct. That run starts without a web context, and
 * in-process is the only place to assert it — the launched process of
 * {@code StdioTransportIT} cannot be observed from here.
 *
 * <p>Both halves of the condition are asserted here rather than left to the integration
 * tests, because a mutation run does not execute those.
 */
class TransportModeValidatorUnitTest {

  private static <T extends ConfigurableApplicationContext> T withStdio(
    T applicationContext,
    String stdio
  ) {
    applicationContext
      .getEnvironment()
      .getPropertySources()
      .addFirst(
        new MapPropertySource("test", Map.of(STDIO_PROPERTY_NAME, stdio))
      );
    return applicationContext;
  }

  @Test
  void aStdioRunHasNoWebContextAndSoNothingToReject() {
    var applicationContext = withStdio(new StaticApplicationContext(), "true");

    assertThatNoException().isThrownBy(() ->
      new TransportModeValidator(applicationContext).afterPropertiesSet()
    );
  }

  @Test
  void aWebRunWithStdioOffIsTheShippedCombination() {
    var applicationContext = withStdio(
      new StaticWebApplicationContext(),
      "false"
    );

    assertThatNoException().isThrownBy(() ->
      new TransportModeValidator(applicationContext).afterPropertiesSet()
    );
  }

  @Test
  void aWebRunWithStdioLeftOnIsRefused() {
    var applicationContext = withStdio(
      new StaticWebApplicationContext(),
      "true"
    );

    assertThatThrownBy(() ->
      new TransportModeValidator(applicationContext).afterPropertiesSet()
    )
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(STDIO_PROPERTY_NAME);
  }
}

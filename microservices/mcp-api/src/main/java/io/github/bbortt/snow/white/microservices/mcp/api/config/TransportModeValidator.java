/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerProperties.CONFIG_PREFIX;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

/**
 * Asserts the two transports stay mutually exclusive. One artifact ships both; a run picks
 * one. Serving HTTP while {@code stdio} is still on would start a standard-stream reader
 * next to the web server and hand the protocol a stdout the logging system also writes to,
 * so that combination fails startup rather than degrading into a server that answers on
 * one transport and corrupts the other.
 */
@RealizesArch(ArchTraceables.ARCH_022_ONE_MODULE_TWO_TRANSPORTS)
@Component
@RequiredArgsConstructor
public class TransportModeValidator implements InitializingBean {

  public static final String STDIO_PROPERTY_NAME = CONFIG_PREFIX + ".stdio";

  private final ApplicationContext applicationContext;

  @Override
  public void afterPropertiesSet() {
    // A stdio run has no web application context, so reaching this with one means the
    // HTTP configuration was started without turning stdio off.
    if (
      applicationContext instanceof WebApplicationContext &&
      applicationContext
        .getEnvironment()
        .getProperty(STDIO_PROPERTY_NAME, Boolean.class, false)
    ) {
      throw new IllegalStateException(
        "Cannot serve the MCP HTTP transport while '%s' is true: set it to false, or start this application with 'spring.main.web-application-type=none'.".formatted(
          STDIO_PROPERTY_NAME
        )
      );
    }
  }
}

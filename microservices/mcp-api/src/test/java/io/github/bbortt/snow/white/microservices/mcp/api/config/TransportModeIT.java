/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.Banner.Mode.OFF;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import io.github.bbortt.snow.white.microservices.mcp.api.Main;
import io.github.bbortt.snow.white.microservices.mcp.api.tool.ReadOnlyToolSetV1;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProviderBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * One artifact, two transports, and never both in one process.
 *
 * <p>Overrides arrive as command-line arguments rather than through
 * {@link SpringApplicationBuilder#properties}, which registers <em>default</em> properties —
 * the lowest-precedence source, and so one that {@code config/application.yaml} outranks.
 */
@VerifiesArch(ArchTraceables.ARCH_022_ONE_MODULE_TWO_TRANSPORTS)
@VerifiesCon(ConTraceables.CON_014_MCP_TOOL_SET_IS_READ_ONLY)
class TransportModeIT {

  private static final String[] CLIENT_BASE_URLS = {
    "--snow.white.mcp.api.api-index.base-url=http://localhost:1",
    "--snow.white.mcp.api.criteria.base-url=http://localhost:1",
    "--snow.white.mcp.api.report.base-url=http://localhost:1",
    // Both ports are pinned in the shipped configuration; a test run takes whatever is free.
    "--server.port=0",
    "--management.server.port=0",
  };

  private static ConfigurableApplicationContext run(String... overrides) {
    return new SpringApplicationBuilder(Main.class)
      .bannerMode(OFF)
      .run(
        concat(stream(CLIENT_BASE_URLS), stream(overrides)).toArray(
          String[]::new
        )
      );
  }

  @Test
  void theShippedConfigurationServesMcpOverHttp() {
    try (var context = run()) {
      assertThat(context).isInstanceOf(WebServerApplicationContext.class);
      assertThat(context.getBeansOfType(McpServerTransportProviderBase.class))
        .as("the streamable HTTP transport")
        .hasSize(1)
        .allSatisfy((name, transport) ->
          assertThat(transport.getClass().getName()).contains("WebMvc")
        );

      assertThat(toolNamesOver(port(context)))
        .as("the read-only tool set an agentic client discovers over HTTP")
        .containsExactlyElementsOf(ReadOnlyToolSetV1.NAMES);
    }
  }

  /** Asks the running server what a client asks, over the transport it ships with. */
  private static List<String> toolNamesOver(int port) {
    var transport = HttpClientStreamableHttpTransport.builder(
      "http://localhost:" + port
    ).build();

    try (var client = McpClient.sync(transport).build()) {
      client.initialize();
      return client
        .listTools()
        .tools()
        .stream()
        .map(McpSchema.Tool::name)
        .sorted()
        .toList();
    }
  }

  private static int port(ConfigurableApplicationContext context) {
    return ((WebServerApplicationContext) context).getWebServer().getPort();
  }

  // The stdio mode has no in-JVM counterpart here: its transport reads System.in until EOF,
  // which inside a test fork means never. StdioTransportIT runs it as its own process.

  @Test
  void servingHttpWithStdioLeftOnFailsStartup() {
    assertThatThrownBy(() -> run("--spring.ai.mcp.server.stdio=true"))
      .rootCause()
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(TransportModeValidator.STDIO_PROPERTY_NAME);
  }
}

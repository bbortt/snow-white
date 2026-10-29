/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.reset;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.Boolean.TRUE;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SysTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSys;
import io.github.bbortt.snow.white.microservices.mcp.api.tool.ReadOnlyToolSetV1;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * An agentic client speaks MCP to the published image over the network.
 *
 * <p>The in-JVM transport tests boot the application themselves and override the ports and
 * the endpoint; this one overrides nothing. What it reaches is whatever the container's own
 * configuration serves, so the pinned {@code /mcp} path, the server port and the active
 * profile are part of what is asserted rather than part of the setup.
 */
class McpApiAppTest {

  private static final String APIS_PATH = "/api/rest/v1/apis";

  private static final String INDEXED_APIS = """
  [{"serviceName":"payments-service","apiName":"payments-api","apiVersion":"1.4.0","sourceUrl":"https://example.test/payments.yaml","apiType":"OPENAPI","prerelease":false,"content":"openapi: 3.1.0"}]""";

  private static String mcpBaseUrl;

  @BeforeAll
  static void beforeAllSetup() {
    mcpBaseUrl =
      "http://" +
      property("mcp-api.host", "localhost") +
      ":" +
      property("mcp-api.port", "8080");

    configureFor(
      property("wiremock.host", "localhost"),
      parseInt(property("wiremock.port", "9000"))
    );
  }

  @BeforeEach
  void beforeEachSetup() {
    reset();
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_022_ONE_MODULE_TWO_TRANSPORTS)
  @VerifiesCon(ConTraceables.CON_014_MCP_TOOL_SET_IS_READ_ONLY)
  void theImageServesTheReadOnlyToolSetOverStreamableHttp() {
    try (var client = connect()) {
      assertThat(client.initialize().serverInfo().name()).isEqualTo(
        "snow-white"
      );

      assertThat(
        client.listTools().tools().stream().map(McpSchema.Tool::name).sorted()
      )
        .as("the tool set an agentic client discovers from the published image")
        .containsExactlyElementsOf(ReadOnlyToolSetV1.NAMES);
    }
  }

  @Test
  @VerifiesSys(SysTraceables.SYS_015_AGENT_QUERYABLE_CONSUMPTION_PATH)
  @VerifiesArch(ArchTraceables.ARCH_021_MCP_SURFACE_IS_A_FACADE)
  void aToolCallReadsThroughToTheRestBackendItFacades() {
    stubFor(get(urlPathEqualTo(APIS_PATH)).willReturn(okJson(INDEXED_APIS)));

    try (var client = connect()) {
      client.initialize();

      // Both parameters are optional, but the request still has to carry an argument map:
      // a null one is rejected before the tool is reached.
      var result = client.callTool(
        McpSchema.CallToolRequest.builder("list_apis").arguments(of()).build()
      );

      assertThat(result.isError())
        .as("tool call failed with %s", result.content())
        .isNotEqualTo(TRUE);
      assertThat(
        result
          .content()
          .stream()
          .filter(McpSchema.TextContent.class::isInstance)
          .map(content -> ((McpSchema.TextContent) content).text())
      )
        .as("the indexed API, as read from the api-index backend")
        .anyMatch(text -> text.contains("payments-service"));
    }

    verify(getRequestedFor(urlPathEqualTo(APIS_PATH)));
  }

  private static McpSyncClient connect() {
    return McpClient.sync(
      HttpClientStreamableHttpTransport.builder(mcpBaseUrl).build()
    ).build();
  }

  private static String property(String name, String fallback) {
    return Optional.ofNullable(getProperty(name)).orElse(fallback);
  }
}

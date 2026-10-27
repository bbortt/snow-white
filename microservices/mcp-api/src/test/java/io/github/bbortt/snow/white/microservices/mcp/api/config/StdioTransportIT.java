/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import io.github.bbortt.snow.white.microservices.mcp.api.tool.ReadOnlyToolSetV1;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * In stdio mode nothing but MCP protocol frames reaches standard output.
 *
 * <p>This runs the packaged application as its own process on purpose. The logging system
 * binds its appenders to the real {@code System.out} at startup, so an in-JVM context says
 * nothing about what a launched process actually prints — and the whole point of the
 * {@code stdio} profile's {@code logback-stdio.xml} is to move that output to stderr.
 */
@VerifiesArch(ArchTraceables.ARCH_022_ONE_MODULE_TWO_TRANSPORTS)
@VerifiesCon(ConTraceables.CON_014_MCP_TOOL_SET_IS_READ_ONLY)
class StdioTransportIT {

  private static final JsonMapper JSON = JsonMapper.builder().build();

  // Generous: this launches a second JVM alongside the one running the test.
  private static final Duration STARTUP_DEADLINE = Duration.ofMinutes(3);

  private static final List<String> REQUESTS = List.of(
    """
    {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"stdio-transport-it","version":"1"}}}""",
    """
    {"jsonrpc":"2.0","method":"notifications/initialized"}""",
    """
    {"jsonrpc":"2.0","id":2,"method":"tools/list"}"""
  );

  @Test
  void aLaunchedStdioProcessPrintsProtocolFramesAndNothingElse()
    throws IOException, InterruptedException {
    var process = new ProcessBuilder(
      Path.of(System.getProperty("java.home"), "bin", "java").toString(),
      "-jar",
      executableJar().toString(),
      "--spring.profiles.active=stdio",
      "--snow.white.mcp.api.api-index.base-url=http://localhost:1",
      "--snow.white.mcp.api.criteria.base-url=http://localhost:1",
      "--snow.white.mcp.api.report.base-url=http://localhost:1"
    )
      // Keep stderr attached: it is where this mode's logging belongs, and it is drained
      // by the surrounding fork rather than filling a pipe nobody reads.
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .start();

    List<String> lines;
    try {
      write(process, REQUESTS);
      lines = readResponses(process, 2);
    } finally {
      process.destroy();
      process.waitFor(30, SECONDS);
    }

    var frames = lines.stream().map(JSON::readTree).toList();

    assertThat(frames)
      .as("every line on stdout is a JSON-RPC frame: %s", lines)
      .isNotEmpty()
      .allSatisfy(frame ->
        assertThat(frame.path("jsonrpc").asString()).isEqualTo("2.0")
      );

    assertThat(
      frameWithId(frames, 1)
        .path("result")
        .path("serverInfo")
        .path("name")
        .asString()
    )
      .as("the initialize handshake completes over standard streams")
      .isEqualTo("snow-white");

    assertThat(
      frameWithId(frames, 2)
        .path("result")
        .path("tools")
        .valueStream()
        .map(tool -> tool.path("name").asString())
        .sorted()
        .toList()
    )
      .as("the same read-only tool set is served over this transport")
      .containsExactlyElementsOf(ReadOnlyToolSetV1.NAMES);
  }

  private static Path executableJar() throws IOException {
    try (var jars = Files.list(Path.of("target"))) {
      return jars
        .filter(path ->
          path.getFileName().toString().endsWith("-executable.jar")
        )
        .findFirst()
        .orElseThrow(() ->
          new IllegalStateException(
            "No repackaged jar in target/; this test runs after the package phase."
          )
        );
    }
  }

  private static void write(Process process, List<String> requests)
    throws IOException {
    var stdin = new OutputStreamWriter(process.getOutputStream(), UTF_8);
    for (var request : requests) {
      stdin.write(request);
      stdin.write('\n');
    }
    stdin.flush();
  }

  /**
   * Reads until the responses to every request carrying an id have arrived, or until the
   * deadline kills the process and closes the stream underneath this loop. A frame that
   * never comes then fails an assertion instead of hanging the build.
   */
  private static List<String> readResponses(
    Process process,
    int expectedResponses
  ) throws IOException {
    CompletableFuture.runAsync(
      process::destroy,
      CompletableFuture.delayedExecutor(STARTUP_DEADLINE.toSeconds(), SECONDS)
    );

    var lines = new ArrayList<String>();
    try (
      var stdout = new BufferedReader(
        new InputStreamReader(process.getInputStream(), UTF_8)
      )
    ) {
      String line;
      var responses = 0;
      while (
        responses < expectedResponses && (line = stdout.readLine()) != null
      ) {
        lines.add(line);
        if (line.contains("\"id\"")) {
          responses++;
        }
      }
    }
    return lines;
  }

  private static JsonNode frameWithId(List<JsonNode> frames, int id) {
    return frames
      .stream()
      .filter(frame -> frame.path("id").asInt(-1) == id)
      .findFirst()
      .orElseThrow(() ->
        new AssertionError(
          "No frame answered request %d: %s".formatted(id, frames)
        )
      );
  }
}

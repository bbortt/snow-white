/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static java.lang.String.format;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@ActiveProfiles("test")
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "tempo.token=test-token",
    "snow.white.openapi.coverage.stream.api-index.base-url=http://localhost:8085",
    "snow.white.openapi.coverage.stream.calculation-request-topic=snow-white-calculation-request",
    "snow.white.openapi.coverage.stream.openapi-calculation-response-topic=snow-white-openapi-calculation-response",
  }
)
public abstract class AbstractTempoOpenApiCoverageServiceIT {

  protected static final int TEMPO_HTTP_PORT = 3200;
  protected static final int TEMPO_OTLP_HTTP_PORT = 4318;

  protected static final GenericContainer<?> TEMPO_CONTAINER =
    new GenericContainer<>(DockerImageName.parse("grafana/tempo:2.6.1"))
      .withCopyFileToContainer(
        MountableFile.forClasspathResource("tempo/tempo.yaml"),
        "/etc/tempo.yaml"
      )
      .withCommand("-config.file=/etc/tempo.yaml")
      .withExposedPorts(TEMPO_HTTP_PORT, TEMPO_OTLP_HTTP_PORT)
      .waitingFor(Wait.forLogMessage(".*Tempo started.*\\n", 1));

  static {
    TEMPO_CONTAINER.start();
  }

  @DynamicPropertySource
  static void tempoProperties(DynamicPropertyRegistry registry) {
    registry.add("tempo.url", () ->
      format(
        "http://%s:%s",
        TEMPO_CONTAINER.getHost(),
        TEMPO_CONTAINER.getMappedPort(TEMPO_HTTP_PORT)
      )
    );
  }
}

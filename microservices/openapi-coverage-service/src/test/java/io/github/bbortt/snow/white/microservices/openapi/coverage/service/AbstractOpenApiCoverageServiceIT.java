/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.AbstractOpenApiCoverageServiceIT.ADMIN_TOKEN;
import static java.lang.String.format;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "influxdb.org=snow-white",
    "influxdb.bucket=raw-data",
    "influxdb.token=" + ADMIN_TOKEN,
    "snow.white.openapi.coverage.service.calculation-request-topic=snow-white-coverage-request",
    "snow.white.openapi.coverage.service.openapi-calculation-response-topic=snow-white-openapi-calculation-response",
  }
)
public abstract class AbstractOpenApiCoverageServiceIT {

  public static final String ADMIN_TOKEN =
    "a9f008a4-c1a1-4984-bdeb-6cf2e52047ed";

  private static final int INFLUX_DB_PORT = 8086;

  static final InfluxDBContainer<?> INFLUX_DB_CONTAINER =
    new InfluxDBContainer<>(DockerImageName.parse("influxdb:2.7.11-alpine"))
      .withAdminToken(ADMIN_TOKEN)
      .withExposedPorts(INFLUX_DB_PORT);

  static {
    INFLUX_DB_CONTAINER.start();
  }

  @DynamicPropertySource
  static void influxDbProperties(DynamicPropertyRegistry registry) {
    registry.add("influxdb.url", () ->
      format(
        "http://%s:%s",
        INFLUX_DB_CONTAINER.getHost(),
        INFLUX_DB_CONTAINER.getMappedPort(INFLUX_DB_PORT)
      )
    );
  }
}

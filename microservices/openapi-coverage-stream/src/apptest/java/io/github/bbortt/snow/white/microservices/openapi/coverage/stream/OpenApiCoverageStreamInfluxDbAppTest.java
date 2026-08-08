/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static java.lang.System.getProperty;

import org.citrusframework.junit.jupiter.CitrusSupport;

/**
 * Runs every shared scenario from {@link AbstractOpenApiCoverageStreamAppTest} against the InfluxDB-backed instance
 * (see {@code pom.xml}'s {@code openapi-coverage-stream-influxdb} image).
 */
@CitrusSupport
class OpenApiCoverageStreamInfluxDbAppTest
  extends AbstractOpenApiCoverageStreamAppTest
{

  @Override
  protected String responseTopic() {
    return getProperty(
      "openapi-calculation-response-influxdb.topic",
      "snow-white-openapi-calculation-response-influxdb"
    );
  }
}

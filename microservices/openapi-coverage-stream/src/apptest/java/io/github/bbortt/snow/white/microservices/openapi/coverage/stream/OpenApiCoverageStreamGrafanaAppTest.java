/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static java.lang.System.getProperty;

import org.citrusframework.junit.jupiter.CitrusSupport;

/**
 * Runs every shared scenario from {@link AbstractOpenApiCoverageStreamAppTest} against the Grafana Tempo-backed instance
 * (see {@code pom.xml}'s {@code openapi-coverage-stream-grafana} image),
 * which is routed through Grafana's authenticated datasource proxy rather than Tempo's raw port.
 */
@CitrusSupport
class OpenApiCoverageStreamGrafanaAppTest
  extends AbstractOpenApiCoverageStreamAppTest
{

  @Override
  protected String responseTopic() {
    return getProperty(
      "openapi-calculation-response-grafana.topic",
      "snow-white-openapi-calculation-response-grafana"
    );
  }
}

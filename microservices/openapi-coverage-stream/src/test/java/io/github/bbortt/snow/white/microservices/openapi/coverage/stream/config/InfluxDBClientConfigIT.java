/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InfluxDBClientConfigIT extends AbstractOpenApiCoverageServiceIT {

  private InfluxDBClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new InfluxDBClientConfig();
  }

  @Nested
  class InfluxDBClient {

    @Autowired
    private InfluxDBProperties influxDBProperties;

    @Test
    void shouldReturnBean() {
      var influxDBPropertiesSpy = spy(influxDBProperties);

      assertThat(fixture.influxDBClient(influxDBPropertiesSpy)).isNotNull();

      verify(influxDBPropertiesSpy).getUrl();
      verify(influxDBPropertiesSpy).getToken();
      verify(influxDBPropertiesSpy).getOrg();
      verify(influxDBPropertiesSpy).getBucket();
    }
  }
}

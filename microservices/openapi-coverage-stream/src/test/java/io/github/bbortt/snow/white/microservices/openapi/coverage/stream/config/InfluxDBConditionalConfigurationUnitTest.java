/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.influxdb.client.InfluxDBClient;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.InfluxDBTelemetryServiceImpl;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class InfluxDBConditionalConfigurationUnitTest {

  private static final String[] FULLY_CONFIGURED_PROPERTIES = {
    "influxdb.url=http://localhost:8086",
    "influxdb.token=token",
    "influxdb.org=org",
    "influxdb.bucket=bucket",
  };

  private final ApplicationContextRunner contextRunner =
    new ApplicationContextRunner()
      .withUserConfiguration(
        InfluxDBClientConfig.class,
        InfluxDBTelemetryServiceImpl.class
      )
      .withBean(
        InfluxDBProperties.class,
        InfluxDBConditionalConfigurationUnitTest::fullyConfiguredInfluxDBProperties
      )
      .withBean(
        OpenApiCoverageStreamProperties.class,
        OpenApiCoverageStreamProperties::new
      );

  @Test
  void shouldRegisterInfluxDBBeans_whenAllPropertiesSet() {
    contextRunner
      .withPropertyValues(FULLY_CONFIGURED_PROPERTIES)
      .run(context ->
        assertThat(context)
          .hasSingleBean(InfluxDBClient.class)
          .hasSingleBean(InfluxDBTelemetryServiceImpl.class)
      );
  }

  @ParameterizedTest
  @ValueSource(
    strings = {
      "influxdb.url",
      "influxdb.token",
      "influxdb.org",
      "influxdb.bucket",
    }
  )
  void shouldNotRegisterInfluxDBBeans_whenAPropertyIsMissing(
    String propertyToOmit
  ) {
    var properties = new LinkedHashMap<String, String>();
    for (var entry : FULLY_CONFIGURED_PROPERTIES) {
      var key = entry.substring(0, entry.indexOf('='));
      if (!key.equals(propertyToOmit)) {
        properties.put(key, entry);
      }
    }

    contextRunner
      .withPropertyValues(properties.values().toArray(String[]::new))
      .run(context ->
        assertThat(context)
          .doesNotHaveBean(InfluxDBClient.class)
          .doesNotHaveBean(InfluxDBTelemetryServiceImpl.class)
      );
  }

  @Test
  void shouldNotRegisterInfluxDBBeans_whenNoPropertiesSet() {
    contextRunner.run(context ->
      assertThat(context)
        .doesNotHaveBean(InfluxDBClient.class)
        .doesNotHaveBean(InfluxDBTelemetryServiceImpl.class)
    );
  }

  private static InfluxDBProperties fullyConfiguredInfluxDBProperties() {
    var properties = new InfluxDBProperties();
    properties.setUrl("http://localhost:8086");
    properties.setToken("token");
    properties.setOrg("org");
    properties.setBucket("bucket");
    return properties;
  }
}

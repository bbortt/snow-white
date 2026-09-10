/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.validation;

import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.TempoProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TelemetryBackendPropertiesValidator {

  public TelemetryBackendPropertiesValidator(
    InfluxDBProperties influxDBProperties,
    TempoProperties tempoProperties
  ) {
    var influxDBConfigured = isInfluxDBConfigured(influxDBProperties);
    var tempoConfigured = isTempoConfigured(tempoProperties);

    if (!influxDBConfigured && !tempoConfigured) {
      throw new IllegalArgumentException(
        "No telemetry backend configured! Configure either InfluxDB or Grafana Tempo. Please read the docs."
      );
    }

    if (influxDBConfigured && tempoConfigured) {
      throw new IllegalArgumentException(
        "Multiple telemetry backends configured! Configure either InfluxDB or Grafana Tempo, not both. Please read the docs."
      );
    }

    logger.info(
      "Configuration: {}",
      toMaskedJsonRepresentation(
        influxDBConfigured ? influxDBProperties : tempoProperties
      )
    );
  }

  private boolean isInfluxDBConfigured(InfluxDBProperties influxDBProperties) {
    var anyFieldSet =
      hasText(influxDBProperties.getUrl()) ||
      hasText(influxDBProperties.getToken()) ||
      hasText(influxDBProperties.getOrg()) ||
      hasText(influxDBProperties.getBucket());

    if (!anyFieldSet) {
      return false;
    }

    var fullyConfigured =
      hasText(influxDBProperties.getUrl()) &&
      hasText(influxDBProperties.getToken()) &&
      hasText(influxDBProperties.getOrg()) &&
      hasText(influxDBProperties.getBucket());

    if (!fullyConfigured) {
      throw new IllegalArgumentException(
        "InfluxDB connection not properly configured! Please read the docs."
      );
    }

    return true;
  }

  private boolean isTempoConfigured(TempoProperties tempoProperties) {
    var anyFieldSet =
      hasText(tempoProperties.getUrl()) ||
      hasText(tempoProperties.getUsername()) ||
      hasText(tempoProperties.getPassword()) ||
      hasText(tempoProperties.getToken());

    if (!anyFieldSet) {
      return false;
    }

    var tokenAuth = hasText(tempoProperties.getToken());
    var basicAuth =
      hasText(tempoProperties.getUsername()) &&
      hasText(tempoProperties.getPassword());
    var partialBasicAuth =
      hasText(tempoProperties.getUsername()) !=
      hasText(tempoProperties.getPassword());

    var fullyConfigured =
      hasText(tempoProperties.getUrl()) && tokenAuth != basicAuth;

    if (!fullyConfigured || partialBasicAuth) {
      throw new IllegalArgumentException(
        "Grafana Tempo connection not properly configured! Configure a URL and exactly one authentication method (token, or username/password). Please read the docs."
      );
    }

    return true;
  }
}

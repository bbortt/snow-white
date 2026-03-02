/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.validation;

import static io.github.bbortt.snow.white.commons.logging.LoggingJsonMapper.toMaskedJsonRepresentation;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.InfluxDBProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InfluxDBPropertiesValidator {

  public InfluxDBPropertiesValidator(InfluxDBProperties influxDBProperties) {
    if (
      !hasText(influxDBProperties.getUrl()) ||
      !hasText(influxDBProperties.getToken()) ||
      !hasText(influxDBProperties.getOrg()) ||
      !hasText(influxDBProperties.getBucket())
    ) {
      throw new IllegalArgumentException(
        "InfluxDB connection not properly configured! Please read the docs."
      );
    }

    logger.info(
      "Configuration: {}",
      toMaskedJsonRepresentation(influxDBProperties)
    );
  }
}

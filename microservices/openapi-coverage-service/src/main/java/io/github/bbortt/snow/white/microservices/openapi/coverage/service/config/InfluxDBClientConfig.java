/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDBClientConfig {

  @Bean
  public InfluxDBClient influxDBClient(InfluxDBProperties influxDBProperties) {
    return InfluxDBClientFactory.create(
      influxDBProperties.getUrl(),
      influxDBProperties.getToken().toCharArray(),
      influxDBProperties.getOrg(),
      influxDBProperties.getBucket()
    );
  }
}

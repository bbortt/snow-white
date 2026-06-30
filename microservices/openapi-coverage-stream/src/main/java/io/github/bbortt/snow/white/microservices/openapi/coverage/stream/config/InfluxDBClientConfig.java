/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Slf4j
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

  @EventListener(ApplicationReadyEvent.class)
  public void validateOnStartup(ApplicationReadyEvent event) {
    InfluxDBClient client = event
      .getApplicationContext()
      .getBean("influxDBClient", InfluxDBClient.class);

    try {
      // 1. Liveness check — unauthenticated, just confirms the server responds
      boolean alive = client.ping();
      if (!alive) {
        throw new IllegalStateException(
          "InfluxDB ping failed — server not reachable"
        );
      }

      // 2. Auth check — authenticated endpoint, validates token/org/permissions
      client.getBucketsApi().findBuckets();
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to connect to InfluxDB!", e);
    }
  }
}

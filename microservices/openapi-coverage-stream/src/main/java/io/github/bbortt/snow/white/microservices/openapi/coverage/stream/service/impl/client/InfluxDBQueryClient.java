/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxTable;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * InfluxDB's Java client reports transport failures (connection refused, timeout, ...) and 5xx
 * responses alike as a bare {@code InfluxException} with no HTTP response attached
 * ({@code status() == 0}); anything with a mapped HTTP response uses a dedicated subclass whose
 * status is available via {@code status()}. 4xx responses (bad query, auth, not found, ...) are
 * permanent failures and must not be retried.
 */
@Component
@ConditionalOnProperty(
  prefix = "influxdb",
  name = { "url", "token", "org", "bucket" }
)
public class InfluxDBQueryClient {

  private InfluxDBClient influxDBClient;

  @Autowired
  public void setInfluxDBClient(InfluxDBClient influxDBClient) {
    this.influxDBClient = influxDBClient;
  }

  @Retryable(
    exceptionExpression = "#root instanceof T(com.influxdb.exceptions.InfluxException) and (#root.status() == 0 or #root.status() >= 500)",
    backoff = @Backoff(delay = 200, multiplier = 2)
  )
  public List<FluxTable> query(String fluxQuery) {
    return influxDBClient.getQueryApi().query(fluxQuery);
  }
}

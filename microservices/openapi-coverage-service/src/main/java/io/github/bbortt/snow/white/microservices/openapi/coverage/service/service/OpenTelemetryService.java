/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData.parseOpenTelemetryData;
import static io.opentelemetry.javaagent.shaded.io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;
import static org.springframework.util.CollectionUtils.isEmpty;

import com.influxdb.client.InfluxDBClient;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto.OpenTelemetryData;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb.AttributeFilter;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenTelemetryService {

  private final InfluxDBClient influxDBClient;
  private final InfluxDBProperties influxDBProperties;

  // TODO: Add api name and api version filter

  public Set<OpenTelemetryData> findOpenTelemetryTracingData(
    String serviceName,
    String lookbackRange,
    @Nullable Set<AttributeFilter> attributeFilters
  ) {
    var fluxBuilder = new StringBuilder();
    fluxBuilder
      .append(buildBucketFilter())
      .append(buildRangeFilter(lookbackRange))
      .append(" |> filter(fn: (r) => r._measurement == \"spans\") ")
      .append(buildServiceNameFilter(serviceName))
      .append(" |> filter(fn: (r) => r._field == \"attributes\") ");

    if (!isEmpty(attributeFilters)) {
      attributeFilters.forEach(attributeFilter ->
        fluxBuilder.append(attributeFilter.toFluxString())
      );
    }

    fluxBuilder.append(
      " |> keep(columns: [\"_field\", \"_value\", \"span_id\", \"trace_id\"]) "
    );

    var fluxQuery = fluxBuilder.toString();
    logger.trace("Firing flux query: {}", fluxQuery);

    var fluxTables = influxDBClient.getQueryApi().query(fluxQuery);

    Set<OpenTelemetryData> openTelemetryData = new HashSet<>();
    fluxTables.forEach(fluxTable ->
      fluxTable
        .getRecords()
        .forEach(fluxRecord ->
          openTelemetryData.add(parseOpenTelemetryData(fluxRecord))
        )
    );

    return openTelemetryData;
  }

  private String buildBucketFilter() {
    return "from(bucket: \"" + influxDBProperties.getBucket() + "\") ";
  }

  private String buildRangeFilter(String lookbackRange) {
    return "|> range(start: -" + lookbackRange + ") ";
  }

  private String buildServiceNameFilter(String serviceName) {
    return (
      "|> filter(fn: (r) => r[\"" +
      SERVICE_NAME.getKey() +
      "\"] == \"" +
      serviceName +
      "\") "
    );
  }
}

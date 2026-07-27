/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InfluxDBTelemetryServiceImplIT extends AbstractOpenApiCoverageServiceIT {

  @Autowired
  private InfluxDBTelemetryServiceImpl influxDBTelemetryService;

  @Autowired
  private InfluxDBClient influxDBClient;

  @Test
  void shouldFindRealTracingDataWrittenToInfluxDB() {
    var serviceName = "influx-it-service";
    var apiName = "influx-it-api";
    var apiVersion = "1.0.0";
    var spanId = "3f1a2c9e7d4b8a61";
    var traceId = "f2c79a8d4bce407aa65c1e7289f6febb";

    var eventTime = Instant.now();

    writeSpan(
      serviceName,
      spanId,
      traceId,
      eventTime,
      // language=json
      """
      {"api.name":"%s","api.version":"%s","http.method":"GET"}
      """.formatted(apiName, apiVersion)
    );

    var apiInformation = ApiInformation.builder()
      .serviceName(serviceName)
      .apiName(apiName)
      .apiVersion(apiVersion)
      .apiType(OPENAPI)
      .build();

    var result = influxDBTelemetryService.findOpenTelemetryTracingData(
      apiInformation,
      eventTime.plusSeconds(60).toEpochMilli(),
      "1h",
      emptySet()
    );

    assertThat(result).hasSize(1);

    var data = result.iterator().next();
    assertThat(data.spanId()).isEqualTo(spanId);
    assertThat(data.traceId()).isEqualTo(traceId);
    assertThat(data.attributes().get("http.method").asString()).isEqualTo(
      "GET"
    );
  }

  @Test
  void shouldApplyAttributeFiltersAgainstRealInfluxDB() {
    var serviceName = "influx-it-filter-service";
    var apiName = "influx-it-filter-api";
    var apiVersion = "1.0.0";

    var getSpanId = "1a2b3c4d5e6f7081";
    var getTraceId = "1a2b3c4d5e6f70811a2b3c4d5e6f7081";
    var postSpanId = "9182736455647382";
    var postTraceId = "918273645564738291827364556473";

    var eventTime = Instant.now();

    writeSpan(
      serviceName,
      getSpanId,
      getTraceId,
      eventTime,
      // language=json
      """
      {"api.name":"%s","api.version":"%s","http.method":"GET"}
      """.formatted(apiName, apiVersion)
    );
    writeSpan(
      serviceName,
      postSpanId,
      postTraceId,
      eventTime,
      // language=json
      """
      {"api.name":"%s","api.version":"%s","http.method":"POST"}
      """.formatted(apiName, apiVersion)
    );

    var apiInformation = ApiInformation.builder()
      .serviceName(serviceName)
      .apiName(apiName)
      .apiVersion(apiVersion)
      .apiType(OPENAPI)
      .build();

    var result = influxDBTelemetryService.findOpenTelemetryTracingData(
      apiInformation,
      eventTime.plusSeconds(60).toEpochMilli(),
      "1h",
      Set.of(new AttributeFilter("http.method", STRING_EQUALS, "GET"))
    );

    assertThat(result).hasSize(1);

    var data = result.iterator().next();
    assertThat(data.spanId()).isEqualTo(getSpanId);
    assertThat(data.traceId()).isEqualTo(getTraceId);
    assertThat(data.attributes().get("http.method").asString()).isEqualTo(
      "GET"
    );
  }

  private void writeSpan(
    String serviceName,
    String spanId,
    String traceId,
    Instant eventTime,
    String attributesJson
  ) {
    var point = Point.measurement("spans")
      .addTag("service.name", serviceName)
      .addTag("span_id", spanId)
      .addTag("trace_id", traceId)
      .addField("attributes", attributesJson)
      .time(eventTime, WritePrecision.NS);

    influxDBClient.getWriteApiBlocking().writePoint(point);
  }
}

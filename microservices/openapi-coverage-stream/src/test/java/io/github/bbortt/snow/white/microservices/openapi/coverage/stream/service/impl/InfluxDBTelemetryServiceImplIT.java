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

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.exception.TelemetryBackendUnavailableException;
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
  void shouldFindRealTracingDataWrittenToInfluxDB()
    throws TelemetryBackendUnavailableException {
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
      emptySet(),
      Set.of("http.method")
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
  void shouldApplyAttributeFiltersAgainstRealInfluxDB()
    throws TelemetryBackendUnavailableException {
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
      Set.of(new AttributeFilter("http.method", STRING_EQUALS, "GET")),
      Set.of("http.method")
    );

    assertThat(result).hasSize(1);

    var data = result.iterator().next();
    assertThat(data.spanId()).isEqualTo(getSpanId);
    assertThat(data.traceId()).isEqualTo(getTraceId);
    assertThat(data.attributes().get("http.method").asString()).isEqualTo(
      "GET"
    );
  }

  @Test
  @RealizesSw(
    SwTraceables.SW_023_INFLUXDB_QUERY_NARROWS_ATTRIBUTES_TO_REQUIRED_KEYS
  )
  void shouldReturnNoAttributeOutsideTheRequiredKeys()
    throws TelemetryBackendUnavailableException {
    var serviceName = "influx-it-narrowing-service";
    var apiName = "influx-it-narrowing-api";
    var apiVersion = "1.0.0";
    var spanId = "5c6d7e8f90a1b2c3";
    var traceId = "5c6d7e8f90a1b2c35c6d7e8f90a1b2c3";

    var eventTime = Instant.now();

    writeSpan(
      serviceName,
      spanId,
      traceId,
      eventTime,
      // language=json
      """
      {"api.name":"%s","api.version":"%s","http.method":"GET","http.path":"/pets","db.statement":"select 1"}
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
      emptySet(),
      // `url.query` is required but absent from the span, which must not fail the query.
      Set.of("http.method", "url.query")
    );

    assertThat(result)
      .singleElement()
      .satisfies(data -> {
        assertThat(data.attributes().get("http.method").asString()).isEqualTo(
          "GET"
        );
        assertThat(data.attributes().propertyNames()).containsExactly(
          "http.method"
        );
      });
  }

  /**
   * The documented {@code content-type} header capture made every calculation on this backend fail
   * with {@code cannot convert [string] to string}: {@code http.request.header.<key>} is an array
   * in the OpenTelemetry semantic conventions, Flux's {@code string()} cannot convert a composite
   * value, and the resulting runtime error inside {@code map} takes down the whole query rather
   * than the one span.
   */
  @Test
  void shouldReturnAnArrayValuedAttributeAsAnArray()
    throws TelemetryBackendUnavailableException {
    var serviceName = "influx-it-array-service";
    var apiName = "influx-it-array-api";
    var apiVersion = "1.0.0";
    var spanId = "7e8f90a1b2c3d4e5";
    var traceId = "7e8f90a1b2c3d4e57e8f90a1b2c3d4e5";

    var eventTime = Instant.now();

    writeSpan(
      serviceName,
      spanId,
      traceId,
      eventTime,
      // language=json
      """
      {"api.name":"%s","api.version":"%s","http.method":"GET","http.request.header.content-type":["application/json"],"http.response.status_code":200,"url.query":"a=1&b=2"}
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
      emptySet(),
      Set.of(
        "http.method",
        "http.request.header.content-type",
        "http.response.status_code",
        "url.query"
      )
    );

    assertThat(result)
      .singleElement()
      .satisfies(data -> {
        var header = data.attributes().get("http.request.header.content-type");
        assertThat(header.isArray()).isTrue();
        assertThat(header)
          .singleElement()
          .satisfies(element ->
            assertThat(element.asString()).isEqualTo("application/json")
          );

        // The scalars the same query projects are unaffected by the encoding.
        assertThat(data.attributes().get("http.method").asString()).isEqualTo(
          "GET"
        );
        assertThat(
          data.attributes().get("http.response.status_code").asString()
        ).isEqualTo("200");

        // The encoder escapes `&` as `&`; a query string that did not survive the decode
        // would split into one token and silently cost every query parameter its coverage.
        assertThat(data.attributes().get("url.query").asString()).isEqualTo(
          "a=1&b=2"
        );
      });
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

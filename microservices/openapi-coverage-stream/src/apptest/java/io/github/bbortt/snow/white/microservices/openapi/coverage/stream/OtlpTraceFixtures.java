/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static java.time.Instant.now;
import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds OTLP/JSON {@code ExportTraceServiceRequest} payloads (protobuf JSON mapping - bytes
 * fields such as {@code traceId}/{@code spanId} are base64) for publishing directly onto the
 * {@code snow-white_outbound} topic, exactly as the collector's {@code kafka/snow-white} receiver
 * (encoding {@code otlp_json}) expects. Skips otel-event-filter-stream entirely, going straight to
 * the collector, which persists to both InfluxDB and Grafana Tempo (see
 * src/apptest/resources/otel-collector/config.yaml).
 */
@NoArgsConstructor(access = PRIVATE)
public final class OtlpTraceFixtures {

  private static final SecureRandom RANDOM = new SecureRandom();

  public static String traceRequestJson(Span... spans) {
    List<Map<String, Object>> resourceSpans = new ArrayList<>();
    for (var span : spans) {
      var resourceAttributes = new ArrayList<>();
      resourceAttributes.add(attribute("service.name", span.serviceName));
      resourceAttributes.addAll(
        span.resourceAttributes
          .entrySet()
          .stream()
          .map(entry -> attribute(entry.getKey(), entry.getValue()))
          .toList()
      );
      resourceSpans.add(
        Map.of(
          "resource",
          Map.of("attributes", resourceAttributes),
          "scopeSpans",
          singletonList(
            Map.of("scope", Map.of(), "spans", singletonList(spanNode(span)))
          )
        )
      );
    }

    return JsonMapper.shared().writeValueAsString(
      Map.of("resourceSpans", resourceSpans)
    );
  }

  private static Map<String, Object> spanNode(Span span) {
    var end = now();
    var start = end.minusSeconds(1);

    List<Map<String, Object>> attributes = new ArrayList<>();
    span.attributes.forEach((key, value) ->
      attributes.add(attribute(key, value))
    );

    Map<String, Object> node = new LinkedHashMap<>();
    node.put("traceId", randomId(16));
    node.put("spanId", randomId(8));
    node.put("name", span.name);
    node.put("startTimeUnixNano", nanos(start));
    node.put("endTimeUnixNano", nanos(end));
    node.put("attributes", attributes);
    return node;
  }

  private static Map<String, Object> attribute(String key, String value) {
    return Map.of("key", key, "value", Map.of("stringValue", value));
  }

  private static String nanos(Instant instant) {
    return Long.toString(
      instant.getEpochSecond() * 1_000_000_000L + instant.getNano()
    );
  }

  /**
   * The collector's kafka receiver ("encoding: otlp_json") uses pdata's own JSON codec for
   * trace/span IDs, which is hex - not the base64 that standard OTLP/JSON (protojson) mapping
   * would use for these bytes fields. Confirmed empirically: base64 (padded or not) fails with
   * "length mismatch" in the collector logs, hex round-trips into InfluxDB correctly.
   */
  private static String randomId(int byteLength) {
    var bytes = new byte[byteLength];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  public static final class Span {

    private final String serviceName;
    private final String name;
    private final Map<String, String> resourceAttributes;
    private final Map<String, String> attributes = new LinkedHashMap<>();

    private Span(String serviceName, String name) {
      this(serviceName, name, new LinkedHashMap<>());
    }

    private Span(
      String serviceName,
      String name,
      Map<String, String> resourceAttributes
    ) {
      this.serviceName = serviceName;
      this.name = name;
      this.resourceAttributes = resourceAttributes;
    }

    public static Span span(String serviceName, String name) {
      return new Span(serviceName, name);
    }

    public static Span span(
      String serviceName,
      String name,
      Map<String, String> resourceAttributes
    ) {
      return new Span(serviceName, name, resourceAttributes);
    }

    public Span attribute(String key, String value) {
      attributes.put(key, value);
      return this;
    }
  }
}

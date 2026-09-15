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
      resourceAttributes.add(
        attribute("service.name", AttributeValue.of(span.serviceName))
      );
      resourceAttributes.addAll(
        span.resourceAttributes
          .entrySet()
          .stream()
          .map(entry ->
            attribute(entry.getKey(), AttributeValue.of(entry.getValue()))
          )
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

  private static Map<String, Object> attribute(
    String key,
    AttributeValue value
  ) {
    return Map.of("key", key, "value", value.toOtlpValue());
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
    private final Map<String, AttributeValue> attributes =
      new LinkedHashMap<>();

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
      attributes.put(key, AttributeValue.of(value));
      return this;
    }

    public Span attribute(String key, long value) {
      attributes.put(key, AttributeValue.of(value));
      return this;
    }

    public Span attribute(String key, double value) {
      attributes.put(key, AttributeValue.of(value));
      return this;
    }

    public Span attribute(String key, boolean value) {
      attributes.put(key, AttributeValue.of(value));
      return this;
    }
  }

  /**
   * An OTLP/JSON attribute value is a typed union (protobuf {@code oneof}) - real instrumentation
   * encodes e.g. {@code http.response.status_code} as {@code intValue}, not {@code stringValue}
   * (see {@code TempoTelemetryServiceImpl#buildAttributes}, which every fixture here must be able
   * to exercise realistically rather than only the string-typed case).
   */
  private sealed interface AttributeValue {
    Map<String, Object> toOtlpValue();

    static AttributeValue of(String value) {
      return new StringValue(value);
    }

    static AttributeValue of(long value) {
      return new IntValue(value);
    }

    static AttributeValue of(double value) {
      return new DoubleValue(value);
    }

    static AttributeValue of(boolean value) {
      return new BoolValue(value);
    }

    record StringValue(String value) implements AttributeValue {
      @Override
      public Map<String, Object> toOtlpValue() {
        return Map.of("stringValue", value);
      }
    }

    // int64 fields serialize as JSON strings under protobuf JSON mapping, even though this is
    // the numeric variant - matches what the collector/Tempo actually emit (see
    // TempoTelemetryServiceImplUnitTest).
    record IntValue(long value) implements AttributeValue {
      @Override
      public Map<String, Object> toOtlpValue() {
        return Map.of("intValue", Long.toString(value));
      }
    }

    record DoubleValue(double value) implements AttributeValue {
      @Override
      public Map<String, Object> toOtlpValue() {
        return Map.of("doubleValue", value);
      }
    }

    record BoolValue(boolean value) implements AttributeValue {
      @Override
      public Map<String, Object> toOtlpValue() {
        return Map.of("boolValue", value);
      }
    }
  }
}

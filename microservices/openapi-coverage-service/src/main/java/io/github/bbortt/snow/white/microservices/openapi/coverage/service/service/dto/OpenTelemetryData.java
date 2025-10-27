/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto;

import static java.util.Objects.requireNonNull;

import com.influxdb.query.FluxRecord;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
public record OpenTelemetryData(
  String spanId,
  String traceId,
  JsonNode attributes
) {
  public static final String SPAN_ID_KEY = "span_id";
  public static final String TRACE_ID_KEY = "trace_id";
  public static final String VALUE_KEY = "_value";

  public static OpenTelemetryData parseOpenTelemetryData(
    FluxRecord fluxRecord
  ) {
    return new OpenTelemetryData(
      requireNonNull(fluxRecord.getValueByKey(SPAN_ID_KEY)).toString(),
      requireNonNull(fluxRecord.getValueByKey(TRACE_ID_KEY)).toString(),
      parseJsonAttributes(
        requireNonNull(fluxRecord.getValueByKey(VALUE_KEY)).toString()
      )
    );
  }

  private static JsonNode parseJsonAttributes(String attributes) {
    var jsonMapper = JsonMapper.shared();

    try {
      return jsonMapper.readTree(attributes);
    } catch (Exception e) {
      logger.warn(
        "Failed parsing span attributes! Returning default empty object.",
        e
      );
      return jsonMapper.createObjectNode();
    }
  }
}

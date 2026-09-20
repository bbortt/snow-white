/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import tools.jackson.databind.JsonNode;

public record OpenTelemetryData(
  String spanId,
  String traceId,
  JsonNode attributes
) {
  public static final String SPAN_ID_KEY = "span_id";
  public static final String TRACE_ID_KEY = "trace_id";
}

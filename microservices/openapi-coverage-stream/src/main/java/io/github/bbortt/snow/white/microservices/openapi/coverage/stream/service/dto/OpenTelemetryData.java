/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import static java.util.Objects.isNull;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record OpenTelemetryData(
  String spanId,
  String traceId,
  JsonNode attributes,
  @Nullable String testCaseName
) {
  public static final String SPAN_ID_KEY = "span_id";
  public static final String TRACE_ID_KEY = "trace_id";

  /**
   * A span as a telemetry backend read it, before its test identity has been hoisted.
   * A backend reads attributes, not configuration, so it cannot know which attribute the operator
   * named for the test identity - {@link #withTestIdentityFrom(String)} resolves that once, on the
   * way into the calculators.
   */
  public OpenTelemetryData(String spanId, String traceId, JsonNode attributes) {
    this(spanId, traceId, attributes, null);
  }

  /**
   * This span with its test identity read out of the given attribute, so that a calculator can
   * name the test that satisfied a target without knowing how the attribute is configured.
   * <p>
   * The value is copied verbatim: never parsed, trimmed, truncated or lowercased. Absent, an
   * explicit null, blank and whitespace-only are all the same thing - no test identity - and all
   * yield {@code null}.
   */
  @RealizesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  public OpenTelemetryData withTestIdentityFrom(String testCaseNameAttribute) {
    return new OpenTelemetryData(
      spanId,
      traceId,
      attributes,
      readTestCaseName(testCaseNameAttribute)
    );
  }

  private @Nullable String readTestCaseName(String testCaseNameAttribute) {
    var attribute = attributes.get(testCaseNameAttribute);

    if (isNull(attribute) || attribute.isNull()) {
      return null;
    }

    var testIdentity = attribute.asString();
    return testIdentity.isBlank() ? null : testIdentity;
  }
}

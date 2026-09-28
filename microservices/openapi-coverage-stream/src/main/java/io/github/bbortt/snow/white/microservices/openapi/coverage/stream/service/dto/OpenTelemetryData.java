/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import static java.nio.charset.StandardCharsets.UTF_8;
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
   * The largest test identity Snow-White stores, in UTF-8 bytes.
   * <p>
   * The upstream convention bounds the value not at all, so something here has to, and the
   * consequence of not bounding it is severe: {@code finding_evidence.test_case_name} is
   * {@code VARCHAR(1024)} and part of a unique constraint, so an over-long name fails the insert,
   * the listener exhausts its retries, and a whole report is discarded over one pathological test
   * name.
   * <p>
   * The bound is in bytes rather than characters because it has to satisfy two limits at once: the
   * column's 1024 <em>characters</em>, and the 2704-byte row limit of the constraint's btree, which
   * 1024 characters would exceed at CJK's 3 bytes each, let alone an emoji's 4. Since no character
   * encodes to less than one byte, 1024 bytes is inside both. That makes it deliberately strict for
   * multi-byte names - a 600-character CJK name the column would hold is still dropped - which is
   * the trade for one rule that cannot fail an insert.
   * <p>
   * Truncating the value is forbidden, so a name past this bound is no identity at all rather than
   * a shortened one: the evidence keeps its trace id and the report survives.
   */
  public static final int MAX_TEST_CASE_NAME_BYTES = 1024;

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
   * yield {@code null}. So does a name too long to store, for the reason on
   * {@link #MAX_TEST_CASE_NAME_BYTES}.
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

    if (testIdentity.isBlank()) {
      return null;
    }

    if (isTooLongToStore(testIdentity)) {
      return null;
    }

    return testIdentity;
  }

  private boolean isTooLongToStore(String testIdentity) {
    return testIdentity.getBytes(UTF_8).length > MAX_TEST_CASE_NAME_BYTES;
  }
}

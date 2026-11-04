/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import static io.github.bbortt.snow.white.commons.event.dto.FindingEvidence.MAX_TEST_CASE_NAME_BYTES;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ConformanceVerdictReader.Warnings;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

public record OpenTelemetryData(
  String spanId,
  String traceId,
  JsonNode attributes,
  @Nullable String testCaseName,
  @Nullable ConformanceVerdict conformanceVerdict
) {
  public static final String SPAN_ID_KEY = "span_id";
  public static final String TRACE_ID_KEY = "trace_id";

  /**
   * A span as a telemetry backend read it, before its test identity and its conformance verdict
   * have been hoisted.
   * A backend reads attributes, not configuration, so it cannot know which attribute the operator
   * named for either - {@link #withTestIdentityFrom(String)} and
   * {@link #withConformanceVerdictFrom(String, String)} resolve that once, on the way into the
   * calculators.
   */
  public OpenTelemetryData(String spanId, String traceId, JsonNode attributes) {
    this(spanId, traceId, attributes, null, null);
  }

  /**
   * A span carrying a test identity and no verdict - every span of every suite that publishes no
   * conformance verdicts, which is all of them until a producer is configured.
   */
  public OpenTelemetryData(
    String spanId,
    String traceId,
    JsonNode attributes,
    @Nullable String testCaseName
  ) {
    this(spanId, traceId, attributes, testCaseName, null);
  }

  /**
   * This span with the conformance verdict it carries read out of the given attributes, so that the
   * conformance calculator judges a verdict rather than a configured attribute name, and every
   * other calculator can tell a harness's span from the application's without reading either.
   *
   * @see ConformanceVerdictReader#readVerdict(String, JsonNode, String, String, Warnings)
   */
  @RealizesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
  public OpenTelemetryData withConformanceVerdictFrom(
    String responseSchemaValidAttribute,
    String responseSchemaViolationsAttribute,
    Warnings warnings
  ) {
    return new OpenTelemetryData(
      spanId,
      traceId,
      attributes,
      testCaseName,
      ConformanceVerdictReader.readVerdict(
        spanId,
        attributes,
        responseSchemaValidAttribute,
        responseSchemaViolationsAttribute,
        warnings
      )
    );
  }

  /**
   * Whether a test harness exported this span rather than the application under test.
   * <p>
   * The verdict is the marker: nothing but a conformance producer emits one, so no second marker
   * attribute is needed. A harness span supplies verdicts to {@code RESPONSE_SCHEMA_CONFORMANCE}
   * and must reach no other criterion, which {@code AbstractOpenApiCoverageCalculator} enforces for
   * every calculator at once.
   */
  @RealizesCon(
    ConTraceables.CON_015_HARNESS_SPAN_NEVER_MOVES_A_COVERAGE_VERDICT
  )
  public boolean isHarnessSpan() {
    return !isNull(conformanceVerdict);
  }

  /**
   * This span with its test identity read out of the given attribute, so that a calculator can
   * name the test that satisfied a target without knowing how the attribute is configured.
   * <p>
   * The value is copied verbatim: never parsed, trimmed, truncated or lowercased. Absent, an
   * explicit null, blank and whitespace-only are all the same thing - no test identity - and all
   * yield {@code null}. So does a name too long to store, for the reason on
   * {@link io.github.bbortt.snow.white.commons.event.dto.FindingEvidence#MAX_TEST_CASE_NAME_BYTES}.
   */
  @RealizesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  public OpenTelemetryData withTestIdentityFrom(String testCaseNameAttribute) {
    return new OpenTelemetryData(
      spanId,
      traceId,
      attributes,
      readTestCaseName(testCaseNameAttribute),
      conformanceVerdict
    );
  }

  /**
   * Whether the given attribute names a test at all, storable or not.
   * <p>
   * {@link #withTestIdentityFrom(String)} yields {@code null} for two very different reasons - the
   * span never named a test, or it named one too long to store - and only the second is worth
   * telling an operator about. Asking here rather than re-reading the attribute keeps both answers
   * derived from {@link #readTestIdentity(String)}, so they cannot drift apart.
   */
  public boolean namesATestIn(String testCaseNameAttribute) {
    return !isNull(readTestIdentity(testCaseNameAttribute));
  }

  private @Nullable String readTestCaseName(String testCaseNameAttribute) {
    var testIdentity = readTestIdentity(testCaseNameAttribute);

    if (isNull(testIdentity) || isTooLongToStore(testIdentity)) {
      return null;
    }

    return testIdentity;
  }

  private @Nullable String readTestIdentity(String testCaseNameAttribute) {
    var attribute = attributes.get(testCaseNameAttribute);

    if (isNull(attribute) || attribute.isNull()) {
      return null;
    }

    var testIdentity = attribute.asString();

    return testIdentity.isBlank() ? null : testIdentity;
  }

  private boolean isTooLongToStore(String testIdentity) {
    return testIdentity.getBytes(UTF_8).length > MAX_TEST_CASE_NAME_BYTES;
  }
}

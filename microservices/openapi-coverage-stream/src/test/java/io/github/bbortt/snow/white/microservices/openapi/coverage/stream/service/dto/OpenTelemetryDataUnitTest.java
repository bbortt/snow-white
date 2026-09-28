/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData.MAX_TEST_CASE_NAME_BYTES;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class OpenTelemetryDataUnitTest {

  private static final String SPAN_ID = "spanId";
  private static final String TRACE_ID = "traceId";

  private static final String TEST_CASE_NAME_ATTRIBUTE = "test.case.name";

  private static ObjectNode attributes() {
    return JsonMapper.shared().createObjectNode();
  }

  @Nested
  class WithTestIdentityFromTest {

    /**
     * A test name is a label, not a structure: the exact bytes the span carried are what a
     * developer has to be able to search their own suite for.
     */
    @ValueSource(
      strings = {
        "org.example.PetstoreIT.shouldRejectUnknownPet",
        "examples/tests/petstore.spec.ts:should reject unknown pet",
        "PetstoreIT#shouldRejectUnknownPet",
        "sollte unbekanntes Haustier ablehnen (überprüft)",
      }
    )
    @ParameterizedTest
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldCopyTheAttributeValueVerbatim(String testCaseName) {
      var attributes = attributes().put(TEST_CASE_NAME_ATTRIBUTE, testCaseName);

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE);

      assertThat(result.testCaseName()).isEqualTo(testCaseName);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldReadTheConfiguredAttributeRatherThanTheConventionalOne() {
      var attributes = attributes().put("custom.test.name", "aTestCase");

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom("custom.test.name");

      assertThat(result.testCaseName()).isEqualTo("aTestCase");
    }

    @Test
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldIgnoreTheConventionalAttribute_whenAnotherOneIsConfigured() {
      var attributes = attributes().put(TEST_CASE_NAME_ATTRIBUTE, "aTestCase");

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom("custom.test.name");

      assertThat(result.testCaseName()).isNull();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldReturnNoTestIdentity_whenAttributeIsAbsent() {
      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes()
      ).withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE);

      assertThat(result.testCaseName()).isNull();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldReturnNoTestIdentity_whenAttributeIsExplicitlyNull() {
      var attributes = attributes();
      attributes.putNull(TEST_CASE_NAME_ATTRIBUTE);

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE);

      assertThat(result.testCaseName()).isNull();
    }

    /**
     * Blank is absent, never an empty name: every consumer falls back to the trace id on null, and
     * an empty string would render as a test that has no name rather than as no test.
     */
    @ValueSource(strings = { "", " ", "\t", "  \n  " })
    @ParameterizedTest
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldReturnNoTestIdentity_whenValueIsBlank(String blank) {
      var attributes = attributes().put(TEST_CASE_NAME_ATTRIBUTE, blank);

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE);

      assertThat(result.testCaseName()).isNull();
    }

    /**
     * The bound is a storage limit, not a convention one, so the longest name that fits is still a
     * name: nothing is dropped until it genuinely cannot be persisted.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldKeepTheTestIdentity_whenItIsExactlyAtTheStorageBound() {
      var testCaseName = "a".repeat(MAX_TEST_CASE_NAME_BYTES);
      var attributes = attributes().put(TEST_CASE_NAME_ATTRIBUTE, testCaseName);

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE);

      assertThat(result.testCaseName()).isEqualTo(testCaseName);
    }

    /**
     * Never truncated: one pathological name costs its own identity, and the trace id it was read
     * from still evidences the match, so the report is not lost with it.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldReturnNoTestIdentity_whenValueExceedsTheStorageBound() {
      var attributes = attributes().put(
        TEST_CASE_NAME_ATTRIBUTE,
        "a".repeat(MAX_TEST_CASE_NAME_BYTES + 1)
      );

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE);

      assertThat(result.testCaseName()).isNull();
    }

    /**
     * Characters would be the wrong unit: 400 of these encode to 1200 bytes, which the column would
     * hold but the unique constraint's btree row limit is measured against.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
    void shouldMeasureTheStorageBoundInBytesRatherThanCharacters() {
      var testCaseName = "測".repeat(400);
      var attributes = attributes().put(TEST_CASE_NAME_ATTRIBUTE, testCaseName);

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE);

      assertThat(testCaseName.length()).isLessThan(MAX_TEST_CASE_NAME_BYTES);
      assertThat(result.testCaseName()).isNull();
    }

    @Test
    void shouldLeaveTheRestOfTheSpanUntouched() {
      var attributes = attributes().put(TEST_CASE_NAME_ATTRIBUTE, "aTestCase");

      var result = new OpenTelemetryData(
        SPAN_ID,
        TRACE_ID,
        attributes
      ).withTestIdentityFrom(TEST_CASE_NAME_ATTRIBUTE);

      assertThat(result).satisfies(
        r -> assertThat(r.spanId()).isEqualTo(SPAN_ID),
        r -> assertThat(r.traceId()).isEqualTo(TRACE_ID),
        r -> assertThat(r.attributes()).isSameAs(attributes)
      );
    }
  }

  @Nested
  class ConstructorTest {

    @Test
    void shouldCarryNoTestIdentity_whenBuiltFromABackendResponse() {
      var result = new OpenTelemetryData(SPAN_ID, TRACE_ID, attributes());

      assertThat(result.testCaseName()).isNull();
    }
  }
}

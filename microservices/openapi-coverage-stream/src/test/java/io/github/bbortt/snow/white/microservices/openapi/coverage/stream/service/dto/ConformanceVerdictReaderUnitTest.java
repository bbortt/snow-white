/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.MAX_VIOLATION_POINTERS;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.MAX_VIOLATION_POINTER_BYTES;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ConformanceVerdictReader.readVerdict;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ConformanceVerdictReader.Warnings;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every reading that is not an unambiguous pass is asserted to be a failure. The attributes arrive
 * as strings from the InfluxDB backend and as their own JSON types from Tempo, so each case is
 * asserted in both shapes where the two differ.
 */
class ConformanceVerdictReaderUnitTest {

  private static final String SPAN_ID = "0123456789abcdef";
  private static final String VALID_ATTRIBUTE = "openapi.response.schema.valid";
  private static final String VIOLATIONS_ATTRIBUTE =
    "openapi.response.schema.violations";

  /**
   * A fresh collector per test: JUnit instantiates the test class for each method, so nothing a
   * reading records leaks into the next assertion.
   */
  private final Warnings warnings = new Warnings();

  @Nested
  class ReadVerdictTest {

    @Test
    void returnsNoVerdict_whenNeitherAttributeIsPresent() {
      var attributes = attributes();

      var result = readVerdict(
        SPAN_ID,
        attributes,
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNull();
    }

    /**
     * A producer emitting an explicit null stated no reading, which is the state of every span of
     * every suite that publishes no verdicts - not a malformed verdict.
     */
    @Test
    void returnsNoVerdict_whenBothAttributesAreExplicitlyNull() {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.putNull(VALID_ATTRIBUTE);
      attributes.putNull(VIOLATIONS_ATTRIBUTE);

      var result = readVerdict(
        SPAN_ID,
        attributes,
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNull();
    }

    @ValueSource(strings = { "true", "TRUE" })
    @ParameterizedTest
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void returnsConforming_whenValidIsTrueWithoutViolations(String valid) {
      var result = readVerdict(
        SPAN_ID,
        attributes(valid, null),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isTrue();
      assertThat(result.violationPointers()).isEmpty();
    }

    /**
     * Tempo keeps an OTLP {@code boolValue} as a JSON boolean, where InfluxDB hands back the string
     * above. Both are the same verdict.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void returnsConforming_whenValidIsABooleanNode() {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put(VALID_ATTRIBUTE, true);

      var result = readVerdict(
        SPAN_ID,
        attributes,
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isTrue();
    }

    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void returnsNonConformingWithEveryPointer_whenValidIsFalse() {
      var result = readVerdict(
        SPAN_ID,
        attributes("false", "[\"/properties/id\",\"/required\"]"),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).containsExactly(
        "/properties/id",
        "/required"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void returnsNonConforming_whenValidIsFalseWithoutViolations() {
      var result = readVerdict(
        SPAN_ID,
        attributes("false", null),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).isEmpty();
    }

    /**
     * The two disagree, and the reading that fails the gate is the one that cannot hide a real
     * break.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void returnsNonConforming_whenValidIsTrueAlongsideViolations() {
      var result = readVerdict(
        SPAN_ID,
        attributes("true", "[\"/properties/id\"]"),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).containsExactly("/properties/id");
    }

    /**
     * Malformed rather than silent: discarding it would lose a failure a validator did find.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void returnsNonConforming_whenViolationsArePresentWithoutValid() {
      var result = readVerdict(
        SPAN_ID,
        attributes(null, "[\"/properties/id\"]"),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).containsExactly("/properties/id");
    }

    @ValueSource(strings = { "yes", "1", "", " ", "truthy" })
    @ParameterizedTest
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void returnsNonConforming_whenValidIsNotABoolean(String valid) {
      var result = readVerdict(
        SPAN_ID,
        attributes(valid, null),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
    }

    @ValueSource(
      strings = {
        "not json at all",
        "{\"/properties/id\":true}",
        "[1,2]",
        "[\"/properties/id\",7]",
        "\"/properties/id\"",
      }
    )
    @ParameterizedTest
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void returnsNonConformingWithoutPointers_whenViolationsAreUnparseable(
      String violations
    ) {
      var result = readVerdict(
        SPAN_ID,
        attributes("false", violations),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).isEmpty();
    }

    /**
     * The bound is applied at read as well as at capture, because the producer contract admits
     * producers Snow-White does not ship and cannot vouch for.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void dropsTheSurplusPointer_whenMoreThanTheBoundArePublished() {
      var violations = range(0, MAX_VIOLATION_POINTERS + 1)
        .mapToObj(index -> "\"/properties/field" + index + "\"")
        .collect(joining(",", "[", "]"));

      var result = readVerdict(
        SPAN_ID,
        attributes("false", violations),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers())
        .hasSize(MAX_VIOLATION_POINTERS)
        .doesNotContain("/properties/field" + MAX_VIOLATION_POINTERS);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void dropsTheOverLongPointer_andKeepsTheRest() {
      var overLong = "/" + "a".repeat(MAX_VIOLATION_POINTER_BYTES);

      var result = readVerdict(
        SPAN_ID,
        attributes("false", "[\"" + overLong + "\",\"/required\"]"),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).containsExactly("/required");
    }

    /**
     * Neither attribute is a correlation key, and neither is required to be the configured name:
     * an operator renaming both is read exactly as the defaults are.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void readsTheConfiguredAttributeNames() {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("custom.valid", "false");
      attributes.put("custom.violations", "[\"/required\"]");
      attributes.put(VALID_ATTRIBUTE, "true");

      var result = readVerdict(
        SPAN_ID,
        attributes,
        "custom.valid",
        "custom.violations",
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.conforming()).isFalse();
      assertThat(result.violationPointers()).containsExactly("/required");
    }
  }

  /**
   * What the reading had to decide for a producer rather than read from it is collected, never
   * logged per span: one misconfigured producer emits the same shape on every span it exports.
   */
  @Nested
  class WarningsTest {

    @Test
    @VerifiesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
    void collectsTheSpan_whenTheValidAttributeHoldsNoBoolean() {
      readVerdict(
        SPAN_ID,
        attributes("1", null),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(warnings.spansWithUnreadableVerdict()).containsExactly(
        SPAN_ID
      );
      assertThat(warnings.spansWithUnusablePointers()).isEmpty();
    }

    @Test
    void collectsTheSpan_whenViolationsAreReportedWithoutAVerdict() {
      readVerdict(
        SPAN_ID,
        attributes(null, "[\"/required\"]"),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(warnings.spansWithUnreadableVerdict()).containsExactly(
        SPAN_ID
      );
    }

    @Test
    void collectsTheSpan_whenViolationsAreNotAJsonArrayOfStrings() {
      readVerdict(
        SPAN_ID,
        attributes("false", "not json"),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(warnings.spansWithUnusablePointers()).containsExactly(SPAN_ID);
      assertThat(warnings.spansWithUnreadableVerdict()).isEmpty();
    }

    @Test
    void collectsTheSpan_whenMorePointersAreNamedThanAVerdictCarries() {
      var pointers = range(0, MAX_VIOLATION_POINTERS + 1)
        .mapToObj(index -> "\"/properties/p" + index + "\"")
        .collect(joining(",", "[", "]"));

      readVerdict(
        SPAN_ID,
        attributes("false", pointers),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(warnings.spansWithUnusablePointers()).containsExactly(SPAN_ID);
    }

    /**
     * A producer naming one broken field twice dropped nothing an operator has to act on: the
     * bound is reported against the pointers a verdict could have carried, not against repetition.
     */
    @Test
    void collectsNothing_whenAPointerIsOnlyRepeated() {
      var result = readVerdict(
        SPAN_ID,
        attributes("false", "[\"/required\",\"/required\"]"),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(result).isNotNull();
      assertThat(result.violationPointers()).containsExactly("/required");
      assertThat(warnings.spansWithUnusablePointers()).isEmpty();
      assertThat(warnings.spansWithUnreadableVerdict()).isEmpty();
    }

    @Test
    void collectsNothing_whenAVerdictReadsAsStated() {
      readVerdict(
        SPAN_ID,
        attributes("false", "[\"/required\"]"),
        VALID_ATTRIBUTE,
        VIOLATIONS_ATTRIBUTE,
        warnings
      );

      assertThat(warnings.spansWithUnreadableVerdict()).isEmpty();
      assertThat(warnings.spansWithUnusablePointers()).isEmpty();
    }
  }

  private static JsonNode attributes() {
    return JsonMapper.shared().createObjectNode();
  }

  private static JsonNode attributes(String valid, String violations) {
    var attributes = JsonMapper.shared().createObjectNode();

    if (valid != null) {
      attributes.put(VALID_ATTRIBUTE, valid);
    }

    if (violations != null) {
      attributes.put(VIOLATIONS_ATTRIBUTE, violations);
    }

    return attributes;
  }
}

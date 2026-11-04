/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.CONFORMING_RESPONSE;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.bounded;
import static io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict.nonConforming;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.ConformanceVerdict;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads a harness's conformance verdict off a span.
 * <p>
 * Every ambiguous reading here resolves to "did not conform", which is the opposite of how the rest
 * of this service treats a missing or malformed attribute. Elsewhere charity costs evidence or
 * leaves a target unjudged; here it would convert a real contract break into a green gate, and a
 * criterion that can be silenced by emitting a malformed attribute is worth less than no criterion.
 * <p>
 * Only an outright absence of both attributes is read as "no verdict", because that is the state of
 * every span of every suite that has not adopted a producer.
 */
@NoArgsConstructor(access = PRIVATE)
@RealizesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
public final class ConformanceVerdictReader {

  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

  /**
   * The spans of one calculation whose verdict could not be read as stated, and those whose
   * violation pointers could not all be kept.
   * <p>
   * Collected rather than logged where they are found: a misconfigured producer emits the same
   * shape on every span it exports, so a line per span would bury the calculation's own log under
   * thousands of identical ones. The caller reports each kind once, for the same reason
   * {@code OpenApiCoverageService} collects the spans whose test identity it had to drop.
   */
  public record Warnings(
    @NonNull List<String> spansWithUnreadableVerdict,
    @NonNull List<String> spansWithUnusablePointers
  ) {
    public Warnings() {
      this(new ArrayList<>(), new ArrayList<>());
    }
  }

  /**
   * The verdict the given span carries, or {@code null} where it carries none.
   * <p>
   * A span holding either attribute holds a verdict: one holding violations without a
   * {@code valid} reading is malformed rather than silent, and reading it as a non-verdict would
   * both lose the failure and hand the span to every other criterion, since the verdict is what
   * marks a span as the harness's.
   */
  public static @Nullable ConformanceVerdict readVerdict(
    @NonNull String spanId,
    @NonNull JsonNode attributes,
    @NonNull String validAttribute,
    @NonNull String violationsAttribute,
    @NonNull Warnings warnings
  ) {
    var valid = presentNode(attributes, validAttribute);
    var violations = presentNode(attributes, violationsAttribute);

    if (isNull(valid) && isNull(violations)) {
      return null;
    }

    var violationPointers = readViolationPointers(violations, spanId, warnings);

    if (isNull(valid)) {
      warnings.spansWithUnreadableVerdict().add(spanId);

      return nonConforming(violationPointers);
    }

    var conforming = readBoolean(valid);

    if (isNull(conforming)) {
      // The value itself is never reported: it is the untrusted part, and the key plus the span id
      // are what an operator needs to find the producer that emitted it.
      warnings.spansWithUnreadableVerdict().add(spanId);

      return nonConforming(violationPointers);
    }

    if (FALSE.equals(conforming)) {
      return nonConforming(violationPointers);
    }

    if (violationPointers.isEmpty()) {
      return CONFORMING_RESPONSE;
    }

    // True alongside violations: the reading that fails cannot hide a real break.
    warnings.spansWithUnreadableVerdict().add(spanId);

    return nonConforming(violationPointers);
  }

  /**
   * The boolean the given attribute states, or {@code null} where it states none.
   * <p>
   * Both forms a telemetry backend can deliver are accepted: Tempo keeps an OTLP {@code boolValue}
   * as a JSON boolean, while InfluxDB stores every attribute as a string and hands back
   * {@code "true"}. Nothing else is read as a boolean - not {@code "1"}, not {@code "yes"} - so a
   * producer emitting a value Snow-White cannot be sure of fails rather than passes.
   */
  private static @Nullable Boolean readBoolean(JsonNode valid) {
    if (valid.isBoolean()) {
      return valid.booleanValue();
    }

    var value = valid.asString();

    if ("true".equalsIgnoreCase(value)) {
      return TRUE;
    }

    return "false".equalsIgnoreCase(value) ? FALSE : null;
  }

  /**
   * The pointers the given attribute lists, bounded.
   * <p>
   * An unparseable value costs the detail and not the verdict: the caller still reads a violation,
   * which is why this returns an empty list rather than refusing.
   */
  private static @NonNull List<String> readViolationPointers(
    @Nullable JsonNode violations,
    String spanId,
    Warnings warnings
  ) {
    if (isNull(violations)) {
      return emptyList();
    }

    List<String> pointers = new ArrayList<>();

    try {
      var parsed = JSON_MAPPER.readTree(violations.asString());

      if (!parsed.isArray()) {
        return unusable(spanId, warnings);
      }

      for (JsonNode pointer : parsed) {
        if (!pointer.isString()) {
          return unusable(spanId, warnings);
        }

        pointers.add(pointer.asString());
      }
    } catch (JacksonException _) {
      return unusable(spanId, warnings);
    }

    var boundedPointers = bounded(pointers);

    // Against the distinct count, not the count read: bounding also drops duplicates, and a
    // producer that named one broken field twice dropped nothing an operator has to act on.
    if (boundedPointers.size() < pointers.stream().distinct().count()) {
      warnings.spansWithUnusablePointers().add(spanId);
    }

    return boundedPointers;
  }

  private static @NonNull List<String> unusable(
    String spanId,
    Warnings warnings
  ) {
    warnings.spansWithUnusablePointers().add(spanId);

    return emptyList();
  }

  /**
   * The attribute under the given key, or {@code null} where the span does not carry it. An
   * explicit JSON null is the same as absent: a producer emitting one named no reading.
   */
  private static @Nullable JsonNode presentNode(
    JsonNode attributes,
    String key
  ) {
    var node = attributes.get(key);

    return isNull(node) || node.isNull() ? null : node;
  }
}

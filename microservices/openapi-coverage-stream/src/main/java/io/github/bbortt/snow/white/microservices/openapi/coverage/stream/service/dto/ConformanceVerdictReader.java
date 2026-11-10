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

    if (isNull(violationPointers)) {
      // A violations attribute that is not a list of pointers states no violation list, which is
      // not a verdict this class can read - whatever `valid` says alongside it. Reading it as "no
      // pointers" would let a producer emitting one malformed attribute report green.
      warnings.spansWithUnreadableVerdict().add(spanId);

      return nonConforming(emptyList());
    }

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
   * Both forms a telemetry backend can deliver are accepted: either reads a producer's OTLP
   * {@code boolValue} back as a JSON boolean, while a producer that stated the verdict as the
   * string {@code "true"} is read as having stated it. Nothing else is read as a boolean - not
   * {@code "1"}, not {@code "yes"} - so a producer emitting a value Snow-White cannot be sure of
   * fails rather than passes.
   * <p>
   * A composite value states no boolean either, and is refused before it is read as text: Jackson
   * raises on {@code asString()} for an array or an object, and this class owes its caller a
   * fail-closed verdict rather than an exception that would cost the whole window's calculation.
   */
  private static @Nullable Boolean readBoolean(JsonNode valid) {
    if (valid.isBoolean()) {
      return valid.booleanValue();
    }

    if (!valid.isValueNode()) {
      return null;
    }

    var value = valid.asString();

    if ("true".equalsIgnoreCase(value)) {
      return TRUE;
    }

    return "false".equalsIgnoreCase(value) ? FALSE : null;
  }

  /**
   * The pointers the given attribute lists, bounded, or {@code null} where it lists none: an empty
   * list and an unreadable attribute are different readings, and only the first is a statement.
   * <p>
   * Losing pointers to the bound costs the detail and not the verdict, because the producer did
   * state a list and the surplus is Snow-White's own limit. A value that is not a list of pointers
   * states nothing the verdict can be read against, and is refused to the caller instead.
   * <p>
   * The producer contract is a string holding a JSON array, and that is what a conforming producer
   * emits; a list arriving as a list is read as the same thing rather than refused, because the
   * backends do not agree on whether an array attribute survives the trip.
   */
  private static @Nullable List<String> readViolationPointers(
    @Nullable JsonNode violations,
    String spanId,
    Warnings warnings
  ) {
    if (isNull(violations)) {
      return emptyList();
    }

    List<String> pointers = new ArrayList<>();

    try {
      // A backend that keeps an attribute's own type hands a list over as a list, so it is read
      // as one rather than through its text: Jackson raises on reading a composite as text.
      var parsed = violations.isArray()
        ? violations
        : JSON_MAPPER.readTree(violations.asString());

      if (!parsed.isArray()) {
        return null;
      }

      for (JsonNode pointer : parsed) {
        if (!pointer.isString()) {
          return null;
        }

        pointers.add(pointer.asString());
      }
    } catch (JacksonException _) {
      return null;
    }

    var boundedPointers = bounded(pointers);

    // Against the distinct count, not the count read: bounding also drops duplicates, and a
    // producer that named one broken field twice dropped nothing an operator has to act on.
    if (boundedPointers.size() < pointers.stream().distinct().count()) {
      warnings.spansWithUnusablePointers().add(spanId);
    }

    return boundedPointers;
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

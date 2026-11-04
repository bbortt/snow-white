/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event.dto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * What a test harness vouched for about one response: whether it matched the schema its document
 * promised, and where it did not.
 * <p>
 * A violation is a JSON Pointer into that response's schema, which is the identity a finding is
 * already given; the validator's own message is deliberately absent, because it is a third party's
 * prose rather than Snow-White's contract.
 * <p>
 * This type is the shape of the verdict, not of an attribute: the span attributes it is read from
 * are the producer contract, and the two bounds below are the part of that contract both ends have
 * to agree on, which is why they live here rather than in either service.
 */
@RealizesSw(SwTraceables.SW_049_CONFORMANCE_VERDICT_ATTRIBUTES)
public record ConformanceVerdict(
  boolean conforming,
  @NonNull List<String> violationPointers
) {
  /**
   * The most violations one verdict carries.
   * <p>
   * A single response can break one field per element of a large array, and without a bound that
   * one response would turn into thousands of findings - an unreadable report and a finding table
   * whose size is set by someone else's payload. The verdict survives the bound: what is dropped is
   * detail about a failure that is still reported as a failure.
   */
  public static final int MAX_VIOLATION_POINTERS = 32;

  /**
   * The longest violation pointer one verdict carries, in UTF-8 bytes.
   * <p>
   * In bytes rather than characters for the reason {@link FindingEvidence#MAX_TEST_CASE_NAME_BYTES}
   * records, and never truncated: half a JSON Pointer addresses a different node than the whole
   * one, so a pointer past this bound is dropped rather than shortened.
   */
  public static final int MAX_VIOLATION_POINTER_BYTES = 512;

  /**
   * The verdict of a response that matched its documented schema.
   */
  public static final ConformanceVerdict CONFORMING_RESPONSE =
    new ConformanceVerdict(true, emptyList());

  /**
   * The verdict of a response that did not, carrying whichever of the given pointers fit inside the
   * two bounds.
   * <p>
   * Applied here rather than at each end so that the bound a producer captures under and the bound
   * a reader admits cannot drift apart: the shipped producer and
   * {@code openapi-coverage-stream} call this same method, and the contract admits producers
   * Snow-White does not ship and cannot vouch for.
   */
  public static @NonNull ConformanceVerdict nonConforming(
    @NonNull List<String> violationPointers
  ) {
    return new ConformanceVerdict(false, bounded(violationPointers));
  }

  /**
   * The given pointers, dropped down to the bounds. Over-long pointers go first, so a verdict does
   * not lose a usable pointer to make room for an unusable one.
   */
  public static @NonNull List<String> bounded(
    @NonNull List<String> violationPointers
  ) {
    return violationPointers
      .stream()
      .filter(
        pointer -> pointer.getBytes(UTF_8).length <= MAX_VIOLATION_POINTER_BYTES
      )
      .distinct()
      .limit(MAX_VIOLATION_POINTERS)
      .toList();
  }
}

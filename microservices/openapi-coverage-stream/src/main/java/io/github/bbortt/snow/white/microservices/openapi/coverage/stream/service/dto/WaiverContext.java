/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The waivers one API test's calculation was dispatched with, and the instant their expiry is
 * judged against.
 * <p>
 * The instant travels beside the waivers rather than being read from whatever clock this pod
 * happens to have, which is what lets the same request answer the same way however much later it
 * is replayed.
 */
@RealizesSw(SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT)
@RealizesCon(ConTraceables.CON_001_DETERMINISTIC_ANALYSIS_RESULTS)
public record WaiverContext(
  @NonNull List<Waiver> waivers,
  @Nullable Instant initiatedAt
) {
  public WaiverContext {
    waivers = List.copyOf(waivers);

    // A dispatch that carries waivers always carries the report's createdAt beside them, so a
    // missing instant here is a producer that moved without this consumer. Neither of the two ways
    // on from that is acceptable - judging expiry against a local clock is the one thing the
    // triggering instant exists to prevent, and dropping the waivers silently publishes a verdict
    // nobody authorised - so the calculation stops instead.
    if (!waivers.isEmpty() && isNull(initiatedAt)) {
      throw new IllegalArgumentException(
        "Cannot judge waiver expiry: the calculation request carries %d waiver(s) but names no instant it was triggered at!".formatted(
          waivers.size()
        )
      );
    }
  }

  /**
   * A calculation nobody waived anything on.
   */
  public static WaiverContext none() {
    return new WaiverContext(List.of(), null);
  }

  public boolean isEmpty() {
    return waivers.isEmpty();
  }

  /**
   * The waivers still in force, which are the only ones that suppress anything.
   */
  @RealizesSw(SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT)
  public List<Waiver> inForce() {
    return waivers
      .stream()
      .filter(waiver -> !hasExpired(waiver))
      .toList();
  }

  /**
   * The waivers that had already expired when the calculation was triggered. They suppress nothing,
   * and they are reported rather than dropped: an expiry exists to force the question back into
   * view, and a criterion that simply starts failing with no mention of the waiver sends its team
   * debugging telemetry instead of reading their waiver file.
   */
  @RealizesSw(SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT)
  public List<Waiver> expired() {
    return waivers.stream().filter(this::hasExpired).toList();
  }

  /**
   * A waiver expiring on the day the calculation was triggered is still in force on that day -
   * {@code expires} names the last day it holds, not the first day it does not.
   */
  private boolean hasExpired(Waiver waiver) {
    return waiver.expires().isBefore(triggeredOn());
  }

  /**
   * The trigger instant as a date, in UTC.
   * <p>
   * A {@code LocalDate} has to be read in some zone, and the pod's own is the one choice that would
   * make the verdict depend on where the calculation ran. UTC is the zone the instant was already
   * stored in.
   */
  private LocalDate triggeredOn() {
    return requireNonNull(initiatedAt).atZone(UTC).toLocalDate();
  }
}

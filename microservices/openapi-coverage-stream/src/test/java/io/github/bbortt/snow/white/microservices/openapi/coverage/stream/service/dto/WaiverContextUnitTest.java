/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WaiverContextUnitTest {

  private static final LocalDate EXPIRES = LocalDate.of(2026, 6, 15);

  private static Waiver waiverExpiring(LocalDate expires) {
    return new Waiver(
      "/pets/{petId}",
      "GET",
      "500",
      null,
      null,
      null,
      "mocked downstream cannot be made to fail",
      expires
    );
  }

  /**
   * The trigger instant as the stream receives it: an {@code Instant}, which only becomes a date
   * once a zone is chosen.
   */
  private static Instant triggeredOn(LocalDate date, LocalTime time) {
    return date.atTime(time).toInstant(UTC);
  }

  @Nested
  class NoneTest {

    @Test
    void shouldCarryNoWaiversAndNoInstant() {
      var result = WaiverContext.none();

      assertThat(result.waivers()).isEmpty();
      assertThat(result.initiatedAt()).isNull();
      assertThat(result.isEmpty()).isTrue();
      assertThat(result.inForce()).isEmpty();
      assertThat(result.expired()).isEmpty();
    }
  }

  @Nested
  class ConstructorTest {

    /**
     * The one failure mode worth stopping the calculation for: a producer that dispatched waivers
     * without the instant they are judged against. Both ways on from there publish a verdict
     * nobody authorised - one against an ambient clock, the other by dropping the waivers - so
     * neither is taken.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldRejectWaivers_withoutAnInstantToJudgeThemAgainst() {
      var waivers = List.of(waiverExpiring(EXPIRES));

      assertThatThrownBy(() -> new WaiverContext(waivers, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("1 waiver(s)")
        .hasMessageContaining("names no instant");
    }

    /**
     * An absent instant is only a contradiction beside a waiver. A calculation nobody waived
     * anything on has nothing to judge, which is the shape {@link WaiverContext#none()} relies on.
     */
    @Test
    void shouldAcceptNoInstant_withoutAnyWaiver() {
      var result = new WaiverContext(List.of(), null);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void shouldDetachTheWaiversFromTheListItWasHanded() {
      List<Waiver> mutable = new ArrayList<>();
      mutable.add(waiverExpiring(EXPIRES));

      var result = new WaiverContext(
        mutable,
        triggeredOn(EXPIRES, LocalTime.NOON)
      );
      mutable.clear();

      assertThat(result.waivers()).hasSize(1);
    }
  }

  @Nested
  class ExpiryTest {

    /**
     * The straightforward direction: a waiver outliving the trigger suppresses what it addresses.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldHoldAWaiverInForce_whenItExpiresAfterTheTriggeringInstant() {
      var waiver = waiverExpiring(EXPIRES);

      var result = new WaiverContext(
        List.of(waiver),
        triggeredOn(EXPIRES.minusDays(1), LocalTime.MAX)
      );

      assertThat(result.inForce()).containsExactly(waiver);
      assertThat(result.expired()).isEmpty();
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldHoldAWaiverExpired_whenItExpiredBeforeTheTriggeringInstant() {
      var waiver = waiverExpiring(EXPIRES);

      var result = new WaiverContext(
        List.of(waiver),
        triggeredOn(EXPIRES.plusDays(1), LocalTime.MIDNIGHT)
      );

      assertThat(result.expired()).containsExactly(waiver);
      assertThat(result.inForce()).isEmpty();
    }

    /**
     * {@code expires} names the last day the waiver holds, not the first day it does not - the
     * reading a human writing a date into a waiver file expects, and the boundary the two tests
     * either side of this one leave open.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldHoldAWaiverInForce_whenItExpiresExactlyOnTheTriggeringDate() {
      var waiver = waiverExpiring(EXPIRES);

      assertThat(
        new WaiverContext(
          List.of(waiver),
          triggeredOn(EXPIRES, LocalTime.MIDNIGHT)
        ).inForce()
      ).containsExactly(waiver);

      assertThat(
        new WaiverContext(
          List.of(waiver),
          triggeredOn(EXPIRES, LocalTime.MAX)
        ).inForce()
      ).containsExactly(waiver);
    }

    /**
     * The replay claim, stated as the two contexts a replay produces: the same waiver against the
     * instant the report was originally triggered at, and against the instant a later re-run would
     * have used. The verdict follows the pinned instant, so a calculation re-derived from a
     * backdated report reproduces the original - including, as here, for a waiver that has since
     * expired.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    @VerifiesCon(ConTraceables.CON_001_DETERMINISTIC_ANALYSIS_RESULTS)
    void shouldJudgeAReplayAgainstThePinnedInstantRatherThanALaterOne() {
      var waiver = waiverExpiring(EXPIRES);
      var pinned = triggeredOn(EXPIRES.minusDays(30), LocalTime.NOON);
      var muchLater = triggeredOn(EXPIRES.plusYears(1), LocalTime.NOON);

      var replayedAsPinned = new WaiverContext(List.of(waiver), pinned);
      var reTriggeredToday = new WaiverContext(List.of(waiver), muchLater);

      assertThat(replayedAsPinned.inForce()).containsExactly(waiver);
      assertThat(reTriggeredToday.inForce()).isEmpty();
      assertThat(reTriggeredToday.expired()).containsExactly(waiver);
    }

    /**
     * Each waiver is judged on its own date, so one expiry in a request neither suppresses nor
     * revives its neighbours.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    void shouldPartitionWaiversByTheirOwnExpiry() {
      var stillHolding = waiverExpiring(EXPIRES.plusDays(1));
      var expiringToday = waiverExpiring(EXPIRES);
      var longGone = waiverExpiring(EXPIRES.minusYears(1));

      var result = new WaiverContext(
        List.of(stillHolding, expiringToday, longGone),
        triggeredOn(EXPIRES, LocalTime.NOON)
      );

      assertThat(result.inForce()).containsExactly(stillHolding, expiringToday);
      assertThat(result.expired()).containsExactly(longGone);
    }

    /**
     * The zone the instant is read in is UTC and not the pod's, so an instant that falls on
     * different dates in different zones still yields one verdict. Judged on the far side of the
     * boundary in both directions, because a local-zone implementation passes either one alone
     * wherever this happens to run.
     */
    @Test
    @VerifiesSw(
      SwTraceables.SW_043_EXPIRY_JUDGED_AGAINST_THE_TRIGGERING_INSTANT
    )
    @VerifiesCon(ConTraceables.CON_001_DETERMINISTIC_ANALYSIS_RESULTS)
    void shouldReadTheInstantInUtcRatherThanTheZoneItRunsIn() {
      var waiver = waiverExpiring(EXPIRES);

      // 23:30 UTC on the expiry date is already the day after in every zone east of UTC.
      assertThat(
        new WaiverContext(
          List.of(waiver),
          triggeredOn(EXPIRES, LocalTime.of(23, 30))
        ).inForce()
      ).containsExactly(waiver);

      // 00:30 UTC the day after is still the expiry date in every zone west of it.
      assertThat(
        new WaiverContext(
          List.of(waiver),
          triggeredOn(EXPIRES.plusDays(1), LocalTime.of(0, 30))
        ).expired()
      ).containsExactly(waiver);
    }
  }
}

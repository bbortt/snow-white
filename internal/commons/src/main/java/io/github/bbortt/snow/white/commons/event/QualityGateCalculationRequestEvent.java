/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.event;

import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.commons.event.dto.Waiver;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@With
@Getter
@Builder
@EqualsAndHashCode
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public final class QualityGateCalculationRequestEvent {

  @NonNull
  private ApiInformation apiInformation;

  @NonNull
  private String lookbackWindow;

  @Nullable
  private Set<AttributeFilter> attributeFilters;

  /**
   * The waivers of this calculation that govern this one API, already filtered by the dispatch -
   * absent where the request carried none for it.
   */
  @Nullable
  private List<Waiver> waivers;

  /**
   * The instant the calculation was triggered, pinned on the report and carried here rather than
   * inferred downstream: neither the record timestamp, whose meaning is a broker setting, nor the
   * clock of whatever pod picks the record up, is the same value twice.
   */
  @Nullable
  private Instant initiatedAt;
}

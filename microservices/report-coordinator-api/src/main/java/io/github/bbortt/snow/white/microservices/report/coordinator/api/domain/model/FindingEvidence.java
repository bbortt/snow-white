/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One captured match between a finding's target and a trace that exercised it.
 * The pair is the identity — a trace observed twice against one target is one row, not two.
 * {@code testCaseName} stays null until a consumer's test harness emits the convention and the
 * narrowed attribute set requests it.
 */
@Getter
@Builder
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
@RealizesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
public class FindingEvidence {

  @NonNull
  @Size(min = 1, max = 64)
  @Column(nullable = false, updatable = false, length = 64)
  private String traceId;

  @Nullable
  @Size(min = 1, max = 256)
  @Column(updatable = false, length = 256)
  private String testCaseName;
}

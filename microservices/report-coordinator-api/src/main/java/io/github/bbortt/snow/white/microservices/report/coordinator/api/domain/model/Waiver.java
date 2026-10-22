/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * One criterion declared inapplicable to one target of one API, as the request that triggered the
 * calculation named it.
 * <p>
 * Stored as given rather than as applied: the waivers a report answers are the ones it was
 * <em>asked for</em>, reasons included, which is what a reviewer six months later needs and what
 * the findings alone cannot say. {@code apiVersion} therefore holds {@code *} where the request
 * wrote {@code *}, not the versions it reached.
 */
@Getter
@Builder
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
@RealizesSw(SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST)
public class Waiver {

  /**
   * The one wildcard this system admits, and only on {@code apiVersion}: on any other field it
   * would name a target space rather than a target.
   */
  public static final String API_VERSION_WILDCARD = "*";

  @NonNull
  @Size(min = 1, max = 64)
  @Column(nullable = false, updatable = false, length = 64)
  private String serviceName;

  @NonNull
  @Size(min = 1, max = 64)
  @Column(nullable = false, updatable = false, length = 64)
  private String apiName;

  @NonNull
  @Size(min = 1, max = 16)
  @Column(nullable = false, updatable = false, length = 16)
  private String apiVersion;

  @NonNull
  @Size(min = 1, max = 256)
  @Column(nullable = false, updatable = false, length = 256)
  private String path;

  @NonNull
  @Size(min = 1, max = 16)
  @Column(nullable = false, updatable = false, length = 16)
  private String method;

  @Nullable
  @Size(min = 1, max = 16)
  @Column(updatable = false, length = 16)
  private String responseCode;

  @Nullable
  @Size(min = 1, max = 256)
  @Column(updatable = false, length = 256)
  private String parameterName;

  @Nullable
  @Size(min = 1, max = 128)
  @Column(updatable = false, length = 128)
  private String contentType;

  @Nullable
  @Size(min = 1, max = 64)
  @Column(updatable = false, length = 64)
  private String criterion;

  @NonNull
  @Size(min = 1, max = 1024)
  @Column(nullable = false, updatable = false, length = 1024)
  private String reason;

  @NonNull
  @Column(nullable = false, updatable = false)
  private LocalDate expires;

  /**
   * Whether this waiver governs the named API, which is both the question the trigger rejects an
   * unknown API by and the one the per-API dispatch filters on - one rule, so the entry that was
   * accepted is the entry that travels.
   * <p>
   * {@code apiVersion} is the only field admitting {@link #API_VERSION_WILDCARD}, and it is matched
   * on the service and API names alone when it does: the waiver reaches every version of that API
   * in the calculation.
   */
  public boolean appliesTo(
    String serviceName,
    String apiName,
    @Nullable String apiVersion
  ) {
    return (
      this.serviceName.equals(serviceName) &&
      this.apiName.equals(apiName) &&
      (API_VERSION_WILDCARD.equals(this.apiVersion) ||
        this.apiVersion.equals(apiVersion))
    );
  }
}

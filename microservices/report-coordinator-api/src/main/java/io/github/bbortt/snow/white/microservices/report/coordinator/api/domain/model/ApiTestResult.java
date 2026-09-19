/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Identity is the {@code (apiTestCriteria, apiTest)} pair: a redelivered result for a criterion
 * already scored must be recognized as the same entry, not a new one, so it replaces rather than
 * duplicates.
 */
@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@IdClass(ApiTestResult.ApiTestResultId.class)
@EqualsAndHashCode(of = { "apiTestCriteria", "apiTest" })
@RealizesSw(
  SwTraceables.SW_020_REDELIVERED_CRITERION_RESULT_REPLACES_EXISTING_ONE
)
public class ApiTestResult {

  @Id
  @NonNull
  @With(PRIVATE)
  @Size(min = 1, max = 64)
  private String apiTestCriteria;

  @NonNull
  @Column(nullable = false, updatable = false, precision = 3, scale = 2)
  private BigDecimal coverage;

  @NonNull
  @Column(nullable = false, updatable = false)
  private Boolean includedInReport;

  @NonNull
  @Column(nullable = false, updatable = false)
  private Duration duration;

  @Lob
  @Nullable
  @Column(columnDefinition = "TEXT")
  private String additionalInformation;

  @Id
  @NonNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "api_test", nullable = false)
  private ApiTest apiTest;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ApiTestResultId implements Serializable {

    private String apiTestCriteria;
    private Long apiTest;
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static jakarta.persistence.CascadeType.ALL;
import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesSw;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
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

  /**
   * The column name is stated rather than left to the naming strategy: {@code ApiTestFinding}
   * points its composite foreign key here by {@code referencedColumnName}, which Hibernate resolves
   * against the <em>logical</em> name. An implicit name would read {@code apiTestCriteria} there and
   * the mapping would not resolve.
   */
  @Id
  @NonNull
  @With(PRIVATE)
  @Size(min = 1, max = 64)
  @Column(name = "api_test_criteria")
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

  /**
   * The evidence behind {@link #coverage}, which is a cache of what these findings imply rather
   * than a value of its own.
   * A redelivery replaces them wholesale with the result that carried them: nothing from a
   * superseded delivery outlives it.
   */
  @NonNull
  @Builder.Default
  @OneToMany(
    mappedBy = "apiTestResult",
    cascade = { ALL },
    orphanRemoval = true
  )
  @RealizesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
  private final Set<ApiTestFinding> findings = new HashSet<>();

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

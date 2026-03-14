/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.reportStatus;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class QualityGateReport {

  @Id
  @NonNull
  @With(PRIVATE)
  @Column(nullable = false, updatable = false)
  private UUID calculationId;

  @NotEmpty
  @Size(min = 1, max = 64)
  @Column(nullable = false, updatable = false, length = 64)
  private String qualityGateConfigName;

  @NonNull
  @OneToOne(
    mappedBy = "qualityGateReport",
    cascade = { ALL },
    fetch = EAGER,
    optional = false
  )
  private ReportParameter reportParameter;

  @NonNull
  @Builder.Default
  @OneToMany(mappedBy = "qualityGateReport", cascade = { ALL }, fetch = EAGER)
  private Set<ApiTest> apiTests = new HashSet<>();

  @NonNull
  @Builder.Default
  @Column(nullable = false)
  private Short reportStatus = IN_PROGRESS.getVal();

  @NonNull
  @Builder.Default
  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Lob
  @Nullable
  @Column(columnDefinition = "TEXT", updatable = false)
  private String stackTrace;

  public @NonNull ReportStatus getReportStatus() {
    return reportStatus(reportStatus);
  }

  public QualityGateReport withReportStatus(
    @NonNull ReportStatus reportStatus
  ) {
    this.reportStatus = reportStatus.getVal();
    return this;
  }
}

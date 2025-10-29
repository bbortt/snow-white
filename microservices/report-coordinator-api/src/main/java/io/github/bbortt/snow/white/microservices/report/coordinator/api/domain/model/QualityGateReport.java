/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.jspecify.annotations.NonNull;

@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class QualityGateReport {

  private static final List<ReportStatus> STATUS_FOR_PROPAGATION = List.of(
    FAILED,
    PASSED
  );

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
  @Enumerated(STRING)
  @Column(nullable = false, length = 16)
  private ReportStatus reportStatus = IN_PROGRESS;

  @NonNull
  @Builder.Default
  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

@Entity
@Table
@With
@Getter
@Builder(buildMethodName = "lombokBuild")
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class QualityGateReport {

  private static final List<ReportStatus> STATUS_FOR_PROPAGATION = List.of(
    FAILED,
    PASSED
  );

  @Id
  @NotNull
  @Column(nullable = false, updatable = false)
  private UUID calculationId;

  @NotEmpty
  @Column(nullable = false, updatable = false)
  private String qualityGateConfigName;

  @NotNull
  @OneToOne(cascade = { ALL }, fetch = EAGER, optional = false)
  private ReportParameters reportParameters;

  @NotNull
  @Builder.Default
  @Enumerated(STRING)
  @Column(nullable = false)
  private ReportStatus openApiCoverageStatus = NOT_STARTED;

  @NotNull
  @Builder.Default
  @OneToMany(mappedBy = "qualityGateReport", cascade = { ALL }, fetch = EAGER)
  private Set<OpenApiTestResult> openApiTestResults = new HashSet<>();

  @NotNull
  @Builder.Default
  @Enumerated(STRING)
  @Column(nullable = false)
  private ReportStatus reportStatus = IN_PROGRESS;

  @NotNull
  @Builder.Default
  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public QualityGateReport withUpdatedReportStatus() {
    ReportStatus updatedStatus = IN_PROGRESS;

    if (
      nonNull(openApiCoverageStatus) &&
      STATUS_FOR_PROPAGATION.contains(openApiCoverageStatus)
    ) {
      updatedStatus = openApiCoverageStatus;
    }

    return withReportStatus(updatedStatus);
  }

  public static class QualityGateReportBuilder {

    public QualityGateReport build() {
      var qualityGateReport = lombokBuild();

      if (isNull(qualityGateReport.getCalculationId())) {
        return qualityGateReport.withCalculationId(randomUUID());
      }

      return qualityGateReport;
    }
  }
}

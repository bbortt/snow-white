/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@IdClass(OpenApiTestResult.OpenApiCriterionResultId.class)
public class OpenApiTestResult {

  @Id
  @NotNull
  private String openApiTestCriteria;

  @NotNull
  @Column(nullable = false, updatable = false)
  private BigDecimal coverage;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Boolean includedInReport;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Duration duration;

  @Column(updatable = false)
  private @Nullable String additionalInformation;

  @Id
  @NotNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "calculation_id", nullable = false)
  private QualityGateReport qualityGateReport;

  @Data
  public static class OpenApiCriterionResultId implements Serializable {

    private String openApiTestCriteria;
    private UUID qualityGateReport;
  }
}

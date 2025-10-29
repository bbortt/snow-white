/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
@IdClass(ApiTestResult.ApiTestResultId.class)
public class ApiTestResult {

  @Id
  @NonNull
  @With(PRIVATE)
  @Size(min = 1, max = 32)
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

  @Size(max = 256)
  @Column(updatable = false, length = 256)
  private @Nullable String additionalInformation;

  @Id
  @NonNull
  @ManyToOne(optional = false)
  @JoinColumn(name = "api_test", nullable = false)
  private ApiTest apiTest;

  @Data
  public static class ApiTestResultId implements Serializable {

    private String apiTestCriteria;
    private Long apiTest;
  }
}

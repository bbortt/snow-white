/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.commons.quality.gate.ApiType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
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
public class ApiTest {

  @Id
  @With(PRIVATE)
  @SequenceGenerator(name = "api_test_id_seq", allocationSize = 1)
  @GeneratedValue(strategy = SEQUENCE, generator = "api_test_id_seq")
  @Column(nullable = false, updatable = false)
  private Long id;

  @NotEmpty
  @Size(min = 1, max = 256)
  @Column(nullable = false, updatable = false, length = 256)
  private String serviceName;

  @NotEmpty
  @Size(min = 1, max = 256)
  @Column(nullable = false, updatable = false, length = 256)
  private String apiName;

  @Size(max = 16)
  @Column(updatable = false, length = 16)
  private @Nullable String apiVersion;

  @Column(updatable = false)
  private @Nullable Short apiType;

  @NonNull
  @Builder.Default
  @OneToMany(mappedBy = "apiTest", cascade = { ALL }, fetch = EAGER)
  private Set<ApiTestResult> apiTestResults = new HashSet<>();

  @ManyToOne(optional = false)
  @JoinColumn(name = "calculation_id", nullable = false)
  private QualityGateReport qualityGateReport;

  public ApiType getApiType() {
    return ApiType.apiType(apiType);
  }

  public ApiTest withApiType(ApiType apiType) {
    return ApiTest.builder()
      .id(getId())
      .serviceName(getServiceName())
      .apiName(getApiName())
      .apiVersion(getApiVersion())
      .apiType(apiType.getVal())
      .apiTestResults(getApiTestResults())
      .qualityGateReport(getQualityGateReport())
      .build();
  }
}

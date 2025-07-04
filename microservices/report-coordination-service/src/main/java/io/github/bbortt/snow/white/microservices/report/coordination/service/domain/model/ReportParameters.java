/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PRIVATE;

import jakarta.annotation.Nullable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
public class ReportParameters {

  @Id
  @NotNull
  @SequenceGenerator(name = "report_parameters_id_seq", allocationSize = 1)
  @GeneratedValue(strategy = SEQUENCE, generator = "report_parameters_id_seq")
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

  @NotEmpty
  @Builder.Default
  @Size(max = 8)
  @Column(nullable = false, updatable = false, length = 8)
  private String lookbackWindow = "1h";

  @Builder.Default
  @ElementCollection
  @Column(name = "attribute_value")
  @MapKeyColumn(name = "attribute_key")
  @CollectionTable(
    name = "attribute_filters",
    joinColumns = @JoinColumn(name = "report_parameter_id")
  )
  private Map<String, String> attributeFilters = new HashMap<>();

  @OneToOne(mappedBy = "reportParameters", optional = false)
  private QualityGateReport qualityGateReport;
}

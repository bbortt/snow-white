/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
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
public class ReportParameter {

  @Id
  @NonNull
  @Column(nullable = false, updatable = false)
  private UUID calculationId;

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
    joinColumns = @JoinColumn(name = "calculation_id")
  )
  private Map<String, String> attributeFilters = new HashMap<>();

  @OneToOne(optional = false)
  @JoinColumn(name = "calculation_id")
  private QualityGateReport qualityGateReport;
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static jakarta.persistence.FetchType.EAGER;
import static lombok.AccessLevel.PRIVATE;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
  private final String lookbackWindow = "1h";

  @Builder.Default
  @ElementCollection(fetch = EAGER)
  @Column(name = "attribute_value")
  @MapKeyColumn(name = "attribute_key")
  @CollectionTable(
    name = "attribute_filters",
    joinColumns = @JoinColumn(name = "calculation_id")
  )
  private final Map<String, String> attributeFilters = new HashMap<>();

  /**
   * The waiver document this calculation was told, in the order the request wrote it.
   * <p>
   * An input beside the lookback window and the attribute filters, not state the system keeps: a
   * calculation that was given none has none, whatever an earlier one was given.
   */
  @Builder.Default
  @RealizesSw(SwTraceables.SW_040_WAIVER_ENTERS_ONLY_ON_THE_CALCULATION_REQUEST)
  @ElementCollection(fetch = EAGER)
  @OrderColumn(name = "waiver_index")
  @CollectionTable(
    name = "waiver",
    joinColumns = @JoinColumn(name = "calculation_id")
  )
  private final List<Waiver> waivers = new ArrayList<>();

  @OneToOne(optional = false)
  @JoinColumn(name = "calculation_id")
  private QualityGateReport qualityGateReport;
}

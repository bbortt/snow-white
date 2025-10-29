/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.CascadeType.REFRESH;
import static jakarta.persistence.FetchType.EAGER;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import org.jspecify.annotations.NonNull;

@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@IdClass(
  QualityGateOpenApiCoverageMapping.QualityGateOpenApiCoverageMappingId.class
)
public class QualityGateOpenApiCoverageMapping {

  @Id
  @NonNull
  @Setter(PACKAGE)
  @ManyToOne(optional = false, cascade = { ALL }, fetch = EAGER)
  @JoinColumn(name = "quality_gate_configuration", nullable = false)
  private QualityGateConfiguration qualityGateConfiguration;

  @Id
  @NonNull
  @ManyToOne(
    optional = false,
    cascade = { PERSIST, MERGE, REFRESH, DETACH },
    fetch = EAGER
  )
  @JoinColumn(name = "open_api_coverage_configuration", nullable = false)
  private OpenApiCoverageConfiguration openApiCoverageConfiguration;

  @Data
  public static class QualityGateOpenApiCoverageMappingId
    implements Serializable {

    private Long qualityGateConfiguration;
    private Long openApiCoverageConfiguration;
  }
}

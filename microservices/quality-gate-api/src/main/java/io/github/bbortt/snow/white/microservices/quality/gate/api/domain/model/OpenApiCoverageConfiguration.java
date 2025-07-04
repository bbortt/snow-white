/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;

@Entity
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
@Table(
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_open_api_coverage", columnNames = { "name" }),
  }
)
public class OpenApiCoverageConfiguration {

  @Id
  @NotNull
  @Column(nullable = false, updatable = false)
  @SequenceGenerator(
    name = "open_api_coverage_configuration_id_seq",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "open_api_coverage_configuration_id_seq"
  )
  private Long id;

  @NotEmpty
  @Size(min = 1, max = 32)
  @Column(nullable = false, updatable = false, unique = true, length = 32)
  private String name;

  @NotNull
  @Builder.Default
  @OneToMany(mappedBy = "openApiCoverageConfiguration")
  private Set<QualityGateOpenApiCoverageMapping> qualityGateConfigurations =
    new HashSet<>();
}

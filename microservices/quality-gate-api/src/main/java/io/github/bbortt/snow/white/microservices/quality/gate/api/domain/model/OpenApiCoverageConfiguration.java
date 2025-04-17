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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
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
public class OpenApiCoverageConfiguration {

  @Id
  @NotNull
  @GeneratedValue(strategy = SEQUENCE)
  @Column(nullable = false, updatable = false)
  private Long id;

  @NotEmpty
  @Column(nullable = false, updatable = false, unique = true)
  private String name;

  @NotNull
  @Builder.Default
  @OneToMany(mappedBy = "openApiCoverageConfiguration")
  private Set<QualityGateOpenApiCoverageMapping> qualityGateConfigurations =
    new HashSet<>();
}

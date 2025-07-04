/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
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
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.With;
import org.springframework.lang.Nullable;

@Entity
@Table(
  uniqueConstraints = {
    @UniqueConstraint(
      name = "uk_quality_gate_configuration_name",
      columnNames = { "name" }
    ),
  }
)
@With
@Getter
@Builder
@Setter(PRIVATE)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class QualityGateConfiguration {

  @Id
  @NotNull
  @Column(nullable = false, updatable = false)
  @SequenceGenerator(
    name = "quality_gate_configuration_id_seq",
    allocationSize = 1
  )
  @GeneratedValue(
    strategy = SEQUENCE,
    generator = "quality_gate_configuration_id_seq"
  )
  private Long id;

  @NotEmpty
  @Column(nullable = false, updatable = false, unique = true)
  private String name;

  private @Nullable String description;

  @NotNull
  @Builder.Default
  @Column(nullable = false, updatable = false)
  private Boolean isPredefined = false;

  @NotNull
  @Builder.Default
  @OneToMany(
    cascade = { ALL },
    fetch = EAGER,
    mappedBy = "qualityGateConfiguration"
  )
  private Set<QualityGateOpenApiCoverageMapping> openApiCoverageConfigurations =
    new HashSet<>();

  public QualityGateConfiguration withId(Long id) {
    setId(id);
    openApiCoverageConfigurations.forEach(mapping ->
      mapping.withQualityGateConfiguration(this)
    );
    return this;
  }

  public QualityGateConfiguration withOpenApiCoverageConfiguration(
    OpenApiCoverageConfiguration openApiCoverageConfiguration
  ) {
    openApiCoverageConfigurations.add(
      QualityGateOpenApiCoverageMapping.builder()
        .qualityGateConfiguration(this)
        .openApiCoverageConfiguration(openApiCoverageConfiguration)
        .build()
    );

    return this;
  }
}

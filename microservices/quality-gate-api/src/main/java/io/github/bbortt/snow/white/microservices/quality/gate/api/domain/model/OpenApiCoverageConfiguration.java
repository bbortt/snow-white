package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
  @Column(nullable = false, updatable = false)
  private String name;

  @Builder.Default
  @OneToMany(mappedBy = "openApiCoverageConfiguration")
  private Set<QualityGateOpenApiCoverageMapping> qualityGateConfigurations =
    new HashSet<>();
}

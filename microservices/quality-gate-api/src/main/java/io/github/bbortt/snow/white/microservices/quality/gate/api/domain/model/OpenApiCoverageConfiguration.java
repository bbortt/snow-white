package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
@IdClass(
  QualityGateOpenApiCoverageMapping.QualityGateOpenApiCoverageMappingId.class
)
public class OpenApiCoverageConfiguration {

  @Id
  @NotEmpty
  @Column(nullable = false, updatable = false)
  private String name;

  @Builder.Default
  @OneToMany(mappedBy = "openApiCoverageConfiguration")
  private Set<QualityGateOpenApiCoverageMapping> qualityGateConfigurations =
    new HashSet<>();

  @Data
  public static class OpenApiCoverageConfigurationId implements Serializable {

    @NotEmpty
    private String name;

    @NotNull
    private Long calculationId;
  }
}

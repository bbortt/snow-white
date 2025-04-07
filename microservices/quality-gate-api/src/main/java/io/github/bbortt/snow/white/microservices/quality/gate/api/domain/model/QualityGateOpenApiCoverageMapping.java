package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

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
public class QualityGateOpenApiCoverageMapping {

  @Id
  @ManyToOne
  @JoinColumn(name = "quality_gate_configuration", nullable = false)
  private QualityGateConfiguration qualityGateConfiguration;

  @Id
  @ManyToOne
  @JoinColumn(name = "open_api_coverage_configuration", nullable = false)
  private OpenApiCoverageConfiguration openApiCoverageConfiguration;

  @Data
  public static class QualityGateOpenApiCoverageMappingId
    implements Serializable {

    private String qualityGateConfiguration;
    private String openApiCoverageConfiguration;
  }
}

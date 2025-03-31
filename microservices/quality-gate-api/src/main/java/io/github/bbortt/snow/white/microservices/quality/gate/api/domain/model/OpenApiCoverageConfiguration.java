package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
  private Long id;

  @Builder.Default
  @Column(nullable = false)
  private Boolean includePathCoverage = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean includeResponseCodeCoverage = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean includeRequiredParameterCoverage = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean includeQueryParameterCoverage = false;

  @Builder.Default
  @Column(nullable = false)
  private Boolean includeHeaderParameterCoverage = false;

  @Builder.Default
  @Column(nullable = false)
  private Boolean includeRequestBodySchemaCoverage = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean includeErrorResponseCoverage = true;

  @Builder.Default
  @Column(nullable = false)
  private Boolean includeContentTypeCoverage = false;
}

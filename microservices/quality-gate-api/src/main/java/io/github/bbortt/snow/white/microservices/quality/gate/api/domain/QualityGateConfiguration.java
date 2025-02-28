package io.github.bbortt.snow.white.microservices.quality.gate.api.domain;

import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.lang.Nullable;

@Entity
@Table
@With
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class QualityGateConfiguration {

  @Id
  private String name;

  private @Nullable String description;

  @Builder.Default
  private Boolean includePathCoverage = true;

  @Builder.Default
  private Boolean includeResponseCodeCoverage = true;

  @Builder.Default
  private Boolean includeRequiredParameterCoverage = true;

  @Builder.Default
  private Boolean includeQueryParameterCoverage = false;

  @Builder.Default
  private Boolean includeHeaderParameterCoverage = false;

  @Builder.Default
  private Boolean includeRequestBodySchemaCoverage = true;

  @Builder.Default
  private Boolean includeErrorResponseCoverage = true;

  @Builder.Default
  private Boolean includeContentTypeCoverage = false;
}

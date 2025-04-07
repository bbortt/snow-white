package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
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
import lombok.Setter;
import lombok.With;
import org.springframework.lang.Nullable;

@Entity
@Table
@With
@Getter
@Builder
@Setter(PRIVATE)
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class QualityGateConfiguration {

  @Id
  @Column(nullable = false, updatable = false)
  private String name;

  private @Nullable String description;

  @Builder.Default
  @OneToMany(
    cascade = { ALL },
    fetch = EAGER,
    mappedBy = "qualityGateConfiguration"
  )
  private Set<QualityGateOpenApiCoverageMapping> openApiCoverageConfigurations =
    new HashSet<>();

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

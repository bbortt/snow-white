package io.github.bbortt.snow.white.microservices.quality.gate.api.domain.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
  @Column(nullable = false, updatable = false)
  private String name;

  private @Nullable String description;

  @OneToOne(cascade = ALL, fetch = EAGER)
  private @Nullable OpenApiCoverageConfiguration openApiCoverageConfiguration;
}

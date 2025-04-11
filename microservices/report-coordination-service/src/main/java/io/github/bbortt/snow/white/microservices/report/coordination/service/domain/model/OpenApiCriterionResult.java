package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
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
public class OpenApiCriterionResult {

  @Id
  @NotEmpty
  @Column(nullable = false, updatable = false)
  private String name;

  @NotNull
  @Column(nullable = false, updatable = false)
  private BigDecimal coverage;

  @NotNull
  @Column(nullable = false, updatable = false)
  private Boolean includedInReport;

  @Id
  @ManyToOne
  @JoinColumn(name = "calculation_id", nullable = false)
  private QualityGateReport qualityGateReport;
}

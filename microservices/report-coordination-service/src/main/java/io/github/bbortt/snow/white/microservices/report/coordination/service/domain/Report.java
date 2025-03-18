package io.github.bbortt.snow.white.microservices.report.coordination.service.domain;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
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
@Builder(buildMethodName = "lombokBuild")
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class Report {

  @Id
  @Column(nullable = false, updatable = false)
  private UUID calculationId;

  @NotEmpty
  @Column(nullable = false, updatable = false)
  private String qualityGateConfigName;

  @NotNull
  @OneToOne(cascade = ALL, fetch = EAGER, optional = false)
  private ReportParameters reportParameters;

  @OneToOne(cascade = ALL, fetch = EAGER)
  private @Nullable OpenApiCoverage openApiCoverage;

  public static class ReportBuilder {

    public Report build() {
      var qualityGateReport = lombokBuild();

      if (isNull(qualityGateReport.getCalculationId())) {
        return qualityGateReport.withCalculationId(randomUUID());
      }

      return qualityGateReport;
    }
  }
}

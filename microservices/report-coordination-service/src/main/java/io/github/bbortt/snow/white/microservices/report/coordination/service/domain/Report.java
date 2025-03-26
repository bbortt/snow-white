package io.github.bbortt.snow.white.microservices.report.coordination.service.domain;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportStatus.PASSED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportStatus.reportStatusFromBooleanValue;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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

  @Builder.Default
  @Enumerated(STRING)
  @Column(nullable = false)
  private ReportStatus reportStatus = IN_PROGRESS;

  public Report withUpdatedReportStatus() {
    ReportStatus updatedStatus = IN_PROGRESS;

    if (nonNull(openApiCoverage)) {
      updatedStatus = reportStatusFromBooleanValue(openApiCoverage.passed());
    }

    return withReportStatus(updatedStatus);
  }

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

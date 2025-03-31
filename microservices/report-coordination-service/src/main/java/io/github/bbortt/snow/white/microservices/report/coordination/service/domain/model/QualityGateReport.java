package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.reportStatusFromBooleanValue;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.SEQUENCE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
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
public class QualityGateReport {

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
  private @Nullable OpenApiCoverageReport openApiCoverageReport;

  @Builder.Default
  @Enumerated(STRING)
  @Column(nullable = false)
  private ReportStatus reportStatus = IN_PROGRESS;

  @Builder.Default
  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public QualityGateReport withUpdatedReportStatus() {
    ReportStatus updatedStatus = IN_PROGRESS;

    if (nonNull(openApiCoverageReport)) {
      updatedStatus = reportStatusFromBooleanValue(
        openApiCoverageReport.passed()
      );
    }

    return withReportStatus(updatedStatus);
  }

  public static class QualityGateReportBuilder {

    public QualityGateReport build() {
      var qualityGateReport = lombokBuild();

      if (isNull(qualityGateReport.getCalculationId())) {
        return qualityGateReport.withCalculationId(randomUUID());
      }

      return qualityGateReport;
    }
  }
}

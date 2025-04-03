package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model;

import static jakarta.persistence.GenerationType.SEQUENCE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.stream;
import static lombok.AccessLevel.PRIVATE;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
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
public class OpenApiCoverageReport {

  @Id
  @GeneratedValue(strategy = SEQUENCE)
  @Column(nullable = false, updatable = false)
  private Long id;

  private BigDecimal pathCoverage;
  private Boolean pathCoverageMet;

  private BigDecimal responseCodeCoverage;
  private Boolean responseCodeCoverageMet;

  private BigDecimal errorResponseCoverage;
  private Boolean errorResponseCoverageMet;

  private BigDecimal requiredParameterCoverage;
  private Boolean requiredParameterCoverageMet;

  private BigDecimal headerParameterCoverage;
  private Boolean headerParameterCoverageMet;

  private BigDecimal queryParameterCoverage;
  private Boolean queryParameterCoverageMet;

  private BigDecimal requestBodySchemaCoverage;
  private Boolean requestBodySchemaCoverageMet;

  private BigDecimal contentTypeCoverage;
  private Boolean contentTypeCoverageMet;

  public boolean passed() {
    return stream(getClass().getDeclaredFields())
      .filter(field -> Boolean.class.equals(field.getType()))
      .allMatch(this::getBooleanValueFromField);
  }

  private boolean getBooleanValueFromField(Field field) {
    try {
      return TRUE.equals(field.get(this));
    } catch (IllegalAccessException e) {
      return false;
    }
  }
}

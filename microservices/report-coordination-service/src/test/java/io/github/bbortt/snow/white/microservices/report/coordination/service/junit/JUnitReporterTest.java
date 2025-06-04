/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.util.StreamUtils.copyToString;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

class JUnitReporterTest {

  private static OpenApiTestResult createOpenApiTestResult(
    String openApiCriterionName,
    BigDecimal coverage,
    Duration duration,
    String additionalInformation
  ) {
    return OpenApiTestResult.builder()
      .openApiTestCriteria(openApiCriterionName)
      .coverage(coverage.setScale(2, HALF_UP))
      .duration(duration)
      .additionalInformation(additionalInformation)
      .build();
  }

  private JUnitReporter fixture;

  @BeforeEach
  void beforeEachSetup() throws JAXBException {
    fixture = new JUnitReporter();
  }

  @Nested
  class TransformToJUnitReport {

    @Test
    void shouldTransformReport_withoutOpenApiCoverage()
      throws IOException, JUnitReportCreationException {
      var qualityGateReport = createInitialQualityGateReport();

      var jUnitReport = fixture.transformToJUnitReport(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterTest/withoutOpenApiCoverage.xml"
      );
    }

    @Test
    void shouldTransformReport_withPassedOpenApiCoverages()
      throws IOException, JUnitReportCreationException {
      var qualityGateReport = createInitialQualityGateReport()
        .withOpenApiTestResults(
          Set.of(
            createOpenApiTestResult(
              PATH_COVERAGE.name(),
              ONE,
              Duration.ofMillis(12345),
              null
            ),
            createOpenApiTestResult(
              HTTP_METHOD_COVERAGE.name(),
              ONE,
              Duration.ofMillis(111213),
              null
            )
          )
        );

      var jUnitReport = fixture.transformToJUnitReport(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterTest/withPassedOpenApiCoverages.xml"
      );
    }

    @Test
    void shouldTransformReport_withFailedOpenApiCoverages()
      throws IOException, JUnitReportCreationException {
      var qualityGateReport = createInitialQualityGateReport()
        .withOpenApiTestResults(
          Set.of(
            createOpenApiTestResult(
              PATH_COVERAGE.name(),
              ZERO,
              Duration.ofMillis(54321),
              "This failed because it can."
            ),
            createOpenApiTestResult(
              HTTP_METHOD_COVERAGE.name(),
              BigDecimal.valueOf(0.5),
              Duration.ofMillis(131211),
              "And this failed because it thought it's cool to do so."
            )
          )
        );

      var jUnitReport = fixture.transformToJUnitReport(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterTest/withFailedOpenApiCoverages.xml"
      );
    }

    @Test
    void shouldTransformReport_withMixedOpenApiCoverages()
      throws IOException, JUnitReportCreationException {
      var qualityGateReport = createInitialQualityGateReport()
        .withOpenApiTestResults(
          Set.of(
            createOpenApiTestResult(
              PATH_COVERAGE.name(),
              ONE,
              Duration.ofMillis(4321),
              null
            ),
            createOpenApiTestResult(
              HTTP_METHOD_COVERAGE.name(),
              BigDecimal.valueOf(0.5),
              Duration.ofMillis(1234),
              "Additional Information."
            )
          )
        );

      var jUnitReport = fixture.transformToJUnitReport(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterTest/withMixedOpenApiCoverages.xml"
      );
    }

    @Test
    void shouldThrow_whenOpenApiCriterionIsInvalid() {
      var invalidName = "invalid";
      var qualityGateReport = createInitialQualityGateReport()
        .withOpenApiTestResults(
          Set.of(
            OpenApiTestResult.builder().openApiTestCriteria(invalidName).build()
          )
        );

      assertThatThrownBy(() ->
        fixture.transformToJUnitReport(qualityGateReport)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("No enum constant")
        .hasMessageEndingWith(invalidName);
    }

    private QualityGateReport createInitialQualityGateReport() {
      return QualityGateReport.builder()
        .qualityGateConfigName(getClass().getSimpleName())
        .createdAt(Instant.parse("2025-04-24T22:30:00.00Z"))
        .build();
    }

    private void verifyJUnitReportEqualsExpectedContent(
      Resource jUnitReport,
      String resourceName
    ) throws IOException {
      assertThat(jUnitReport.getContentAsString(UTF_8)).isEqualTo(
        copyToString(
          getClass().getClassLoader().getResourceAsStream(resourceName),
          UTF_8
        )
      );
    }
  }
}

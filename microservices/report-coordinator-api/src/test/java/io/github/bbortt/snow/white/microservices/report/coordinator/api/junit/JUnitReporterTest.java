/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.util.StreamUtils.copyToString;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.xml.sax.SAXException;

class JUnitReporterTest {

  private static final UUID CALCULATION_ID = UUID.fromString(
    "f8b1c0d2-3e4f-4a5b-8c6d-7e8f9a0b1c2d"
  );

  private static QualityGateReport createInitialQualityGateReport() {
    return QualityGateReport.builder()
      .calculationId(CALCULATION_ID)
      .qualityGateConfigName(JUnitReporterTest.class.getSimpleName())
      .createdAt(Instant.parse("2025-04-24T22:30:00.00Z"))
      .reportParameter(mock(ReportParameter.class))
      .build();
  }

  private static ApiTest createApiTest(
    String apiName,
    Set<ApiTestResult> apiTestResults
  ) {
    return ApiTest.builder()
      .serviceName("serviceName")
      .apiName(apiName)
      .apiVersion("1.0.0")
      .apiType(OPENAPI.getVal())
      .apiTestResults(apiTestResults)
      .build();
  }

  private static ApiTestResult createOpenApiTestResult(
    String openApiCriterionName,
    BigDecimal coverage,
    Duration duration,
    String additionalInformation
  ) {
    return ApiTestResult.builder()
      .apiTestCriteria(openApiCriterionName)
      .coverage(coverage.setScale(2, HALF_UP))
      .includedInReport(TRUE)
      .duration(duration)
      .additionalInformation(additionalInformation)
      .apiTest(mock(ApiTest.class))
      .build();
  }

  private JUnitReporter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new JUnitReporter();
  }

  @Nested
  class TransformToJUnitReport {

    @Test
    void shouldTransformReport_withoutOpenApiCoverage()
      throws IOException, JUnitReportCreationException, SAXException {
      var qualityGateReport = createInitialQualityGateReport();

      var jUnitReport = fixture.transformToJUnitReport(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterTest/withoutOpenApiCoverage.xml"
      );
    }

    @Test
    void shouldTransformReport_withPassedOpenApiCoverages()
      throws IOException, JUnitReportCreationException, SAXException {
      var qualityGateReport = createInitialQualityGateReport().withApiTests(
        Set.of(
          createApiTest(
            "testApi",
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
      throws IOException, JUnitReportCreationException, SAXException {
      var qualityGateReport = createInitialQualityGateReport().withApiTests(
        Set.of(
          createApiTest(
            "testApi",
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
      throws IOException, JUnitReportCreationException, SAXException {
      var qualityGateReport = createInitialQualityGateReport().withApiTests(
        Set.of(
          createApiTest(
            "foo",
            Set.of(
              createOpenApiTestResult(
                PATH_COVERAGE.name(),
                ONE,
                Duration.ofMillis(4321),
                null
              )
            )
          ),
          createApiTest(
            "bar",
            Set.of(
              createOpenApiTestResult(
                HTTP_METHOD_COVERAGE.name(),
                BigDecimal.valueOf(0.5),
                Duration.ofMillis(1234),
                "Additional Information."
              )
            )
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

      var apiTestResultMock = mock(ApiTestResult.class);
      doReturn(invalidName).when(apiTestResultMock).getApiTestCriteria();

      var qualityGateReport = createInitialQualityGateReport().withApiTests(
        Set.of(createApiTest("testApi", Set.of(apiTestResultMock)))
      );

      assertThatThrownBy(() ->
        fixture.transformToJUnitReport(qualityGateReport)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("No enum constant")
        .hasMessageEndingWith(invalidName);
    }

    private void verifyJUnitReportEqualsExpectedContent(
      Resource jUnitReport,
      String resourceName
    ) throws IOException, SAXException {
      assertXMLEqual(
        copyToString(
          getClass().getClassLoader().getResourceAsStream(resourceName),
          UTF_8
        ),
        jUnitReport.getContentAsString(UTF_8)
      );
    }
  }
}

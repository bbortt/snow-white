/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.POSITIVE_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.TestData.defaultApiTest;
import static java.lang.Boolean.FALSE;
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

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.XmlMapperConfiguration;
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
import org.xml.sax.SAXException;
import tools.jackson.dataformat.xml.XmlMapper;

class JUnitReporterUnitTest {

  private static final UUID CALCULATION_ID = UUID.fromString(
    "f8b1c0d2-3e4f-4a5b-8c6d-7e8f9a0b1c2d"
  );

  private static QualityGateReport createInitialQualityGateReport() {
    return createInitialQualityGateReport(100);
  }

  private static QualityGateReport createInitialQualityGateReport(
    int minCoveragePercentage
  ) {
    return QualityGateReport.builder()
      .calculationId(CALCULATION_ID)
      .qualityGateConfigName(JUnitReporterUnitTest.class.getSimpleName())
      .minCoveragePercentage(minCoveragePercentage)
      .createdAt(Instant.parse("2025-04-24T22:30:00.00Z"))
      .reportParameter(mock(ReportParameter.class))
      .build();
  }

  private static ApiTest createApiTest(
    String apiName,
    Set<ApiTestResult> apiTestResults
  ) {
    return defaultApiTest()
      .withApiName(apiName)
      .withApiTestResults(apiTestResults);
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

  private final XmlMapper xmlMapper = new XmlMapperConfiguration().xmlMapper();

  private JUnitReporter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new JUnitReporter();
  }

  @Nested
  class TransformToJUnitTestSuitesTest {

    @Test
    void shouldTransformReport_withoutOpenApiCoverage()
      throws IOException, SAXException {
      var qualityGateReport = createInitialQualityGateReport();

      var jUnitReport = fixture.transformToJUnitTestSuites(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterUnitTest/withoutOpenApiCoverage.xml"
      );
    }

    @Test
    void shouldTransformReport_withPassedOpenApiCoverages()
      throws IOException, SAXException {
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

      var jUnitReport = fixture.transformToJUnitTestSuites(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterUnitTest/withPassedOpenApiCoverages.xml"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
    void shouldTransformReport_withFailedOpenApiCoverages()
      throws IOException, SAXException {
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

      var jUnitReport = fixture.transformToJUnitTestSuites(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterUnitTest/withFailedOpenApiCoverages.xml"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
    void shouldTransformReport_withMixedOpenApiCoverages()
      throws IOException, SAXException {
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

      var jUnitReport = fixture.transformToJUnitTestSuites(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterUnitTest/withMixedOpenApiCoverages.xml"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
    void shouldTransformReport_withCoveragesAroundALoweredThreshold()
      throws IOException, SAXException {
      // Under a gate at 80%, 0.5 is still a failure while 0.85 and 0.9 pass —
      // carrying the gap they still have as a comment.
      var qualityGateReport = createInitialQualityGateReport(80).withApiTests(
        Set.of(
          createApiTest(
            "testApi",
            Set.of(
              createOpenApiTestResult(
                HTTP_METHOD_COVERAGE.name(),
                BigDecimal.valueOf(0.5),
                Duration.ofMillis(1234),
                "Half the documented methods went untested."
              ),
              createOpenApiTestResult(
                PATH_COVERAGE.name(),
                BigDecimal.valueOf(0.85),
                Duration.ofMillis(4321),
                "Two of thirteen paths were never called."
              ),
              // No additional information: the comment stands on its own.
              createOpenApiTestResult(
                POSITIVE_RESPONSE_CODE_COVERAGE.name(),
                BigDecimal.valueOf(0.9),
                Duration.ofMillis(2345),
                null
              )
            )
          )
        )
      );

      var jUnitReport = fixture.transformToJUnitTestSuites(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterUnitTest/withCoveragesAroundALoweredThreshold.xml"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
    void shouldTransformReport_withExcludedOpenApiCoverages()
      throws IOException, SAXException {
      var qualityGateReport = createInitialQualityGateReport();

      // Zero coverage on an excluded criterion: exclusion wins over the coverage
      // bar, so this must not turn up as a failure.
      var excludedApiTestResult = createOpenApiTestResult(
        PATH_COVERAGE.name(),
        ZERO,
        Duration.ofMillis(4321),
        null
      ).withIncludedInReport(FALSE);

      var jUnitReport = fixture.transformToJUnitTestSuites(
        qualityGateReport.withApiTests(
          Set.of(createApiTest("testApi", Set.of(excludedApiTestResult)))
        )
      );

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterUnitTest/withExcludedOpenApiCoverages.xml"
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
        fixture.transformToJUnitTestSuites(qualityGateReport)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageStartingWith("No enum constant")
        .hasMessageEndingWith(invalidName);
    }

    private void verifyJUnitReportEqualsExpectedContent(
      TestSuites testSuites,
      String resourceName
    ) throws IOException, SAXException {
      assertXMLEqual(
        copyToString(
          getClass().getClassLoader().getResourceAsStream(resourceName),
          UTF_8
        ),
        xmlMapper.writeValueAsString(testSuites)
      );
    }
  }
}

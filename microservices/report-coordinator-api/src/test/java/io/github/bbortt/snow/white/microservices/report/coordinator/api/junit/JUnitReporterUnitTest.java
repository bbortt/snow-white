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
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.WAIVED;
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

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.XmlMapperConfiguration;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
    return createOpenApiTestResult(
      openApiCriterionName,
      coverage,
      duration,
      additionalInformation,
      null,
      null
    );
  }

  /**
   * The same result, carrying the two counts the reduction that produced its ratio recorded - which
   * a result scored before they were derived does not have, hence the nullable pair.
   */
  private static ApiTestResult createOpenApiTestResult(
    String openApiCriterionName,
    BigDecimal coverage,
    Duration duration,
    String additionalInformation,
    Integer judgedTargetCount,
    Integer waivedTargetCount
  ) {
    return ApiTestResult.builder()
      .apiTestCriteria(openApiCriterionName)
      .coverage(coverage.setScale(2, HALF_UP))
      .includedInReport(TRUE)
      .duration(duration)
      .additionalInformation(additionalInformation)
      .judgedTargetCount(judgedTargetCount)
      .waivedTargetCount(waivedTargetCount)
      .apiTest(mock(ApiTest.class))
      .build();
  }

  /**
   * Attaches one finding to the result and hands the result back, so a fixture reads as the list of
   * targets its criterion concluded something about.
   */
  private static ApiTestResult withFinding(
    ApiTestResult apiTestResult,
    FindingStatus status,
    String httpMethod,
    String httpPath,
    String responseCode,
    String waiverReason,
    LocalDate waiverExpiresOn
  ) {
    apiTestResult.getFindings().add(
      ApiTestFinding.builder()
        .specPointer("/paths" + httpPath + "/" + httpMethod.toLowerCase())
        .status(status.getVal())
        .httpMethod(httpMethod)
        .httpPath(httpPath)
        .responseCode(responseCode)
        .waiverReason(waiverReason)
        .waiverExpiresOn(waiverExpiresOn)
        .apiTestResult(apiTestResult)
        .build()
    );

    return apiTestResult;
  }

  private static ApiTestResult withWaivedTarget(
    ApiTestResult apiTestResult,
    String httpMethod,
    String httpPath,
    String responseCode,
    String waiverReason,
    LocalDate waiverExpiresOn
  ) {
    return withFinding(
      apiTestResult,
      WAIVED,
      httpMethod,
      httpPath,
      responseCode,
      waiverReason,
      waiverExpiresOn
    );
  }

  private static ApiTestResult withCoveredTarget(
    ApiTestResult apiTestResult,
    String httpMethod,
    String httpPath,
    String responseCode
  ) {
    return withFinding(
      apiTestResult,
      COVERED,
      httpMethod,
      httpPath,
      responseCode,
      null,
      null
    );
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
    @VerifiesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
    @VerifiesSw(
      SwTraceables.SW_044_REPORT_PUBLISHES_JUDGED_AND_WAIVED_TARGET_COUNTS
    )
    @VerifiesCon(
      ConTraceables.CON_012_CRITERION_EMPTIED_BY_WAIVERS_IS_NEVER_A_COVERAGE_PASS
    )
    void shouldTransformReport_withFullCoverageReachedOnlyByWaiver()
      throws IOException, SAXException {
      // Every target this criterion would have judged was waived, so its ratio is
      // a vacuous 1.00. The document must not read as the plain passing case a
      // genuinely complete criterion produces: judgedTargetCount="0" beside a
      // non-zero waivedTargetCount says so to a parser, and system-out names each
      // waived target with the argument it was waived under to a person.
      var apiTestResult = withWaivedTarget(
        withWaivedTarget(
          createOpenApiTestResult(
            PATH_COVERAGE.name(),
            ONE,
            Duration.ofMillis(4321),
            null,
            0,
            2
          ),
          "POST",
          "/pets",
          null,
          "Creation is exercised by the contract suite instead.",
          LocalDate.parse("2026-12-31")
        ),
        "GET",
        "/pets/{petId}",
        "404",
        "The upstream cannot produce a miss in the test environment.",
        LocalDate.parse("2026-11-30")
      );

      var qualityGateReport = createInitialQualityGateReport().withApiTests(
        Set.of(createApiTest("testApi", Set.of(apiTestResult)))
      );

      var jUnitReport = fixture.transformToJUnitTestSuites(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterUnitTest/withFullCoverageReachedOnlyByWaiver.xml"
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
    @VerifiesSw(
      SwTraceables.SW_044_REPORT_PUBLISHES_JUDGED_AND_WAIVED_TARGET_COUNTS
    )
    @VerifiesCon(
      ConTraceables.CON_012_CRITERION_EMPTIED_BY_WAIVERS_IS_NEVER_A_COVERAGE_PASS
    )
    void shouldTransformReport_withACoverageGapBesideAWaiver()
      throws IOException, SAXException {
      // A waiver changes neither verdict: the criterion below the gate still fails
      // and the one above it still passes, each adding its waived targets to the
      // shape it already had. The excluded criterion stays bare `skipped` even
      // though it too holds a waived target, and its waived target stays out of the
      // root total - nothing judged it.
      var failedApiTestResult = withWaivedTarget(
        createOpenApiTestResult(
          HTTP_METHOD_COVERAGE.name(),
          BigDecimal.valueOf(0.5),
          Duration.ofMillis(1234),
          "Half the documented methods went untested.",
          4,
          1
        ),
        "DELETE",
        "/pets/{petId}",
        null,
        "Deletion is destructive and stays out of the shared environment.",
        LocalDate.parse("2027-01-31")
      );

      var passedApiTestResult = withCoveredTarget(
        withWaivedTarget(
          createOpenApiTestResult(
            PATH_COVERAGE.name(),
            BigDecimal.valueOf(0.85),
            Duration.ofMillis(4321),
            "Two of thirteen paths were never called.",
            12,
            1
          ),
          "GET",
          "/pets/{petId}/history",
          null,
          "The history endpoint ships behind a flag that is off here.",
          LocalDate.parse("2026-12-01")
        ),
        "GET",
        "/pets",
        "200"
      );

      var excludedApiTestResult = withWaivedTarget(
        createOpenApiTestResult(
          POSITIVE_RESPONSE_CODE_COVERAGE.name(),
          ZERO,
          Duration.ofMillis(2345),
          null,
          0,
          1
        ),
        "GET",
        "/pets",
        "206",
        "Partial content is not implemented yet.",
        LocalDate.parse("2026-10-31")
      ).withIncludedInReport(FALSE);

      var qualityGateReport = createInitialQualityGateReport(80).withApiTests(
        Set.of(
          createApiTest(
            "testApi",
            Set.of(
              failedApiTestResult,
              passedApiTestResult,
              excludedApiTestResult
            )
          )
        )
      );

      var jUnitReport = fixture.transformToJUnitTestSuites(qualityGateReport);

      verifyJUnitReportEqualsExpectedContent(
        jUnitReport,
        "JUnitReporterUnitTest/withACoverageGapBesideAWaiver.xml"
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

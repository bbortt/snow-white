/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.API_NAME;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.API_VERSION;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.CALCULATION_ID;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.DESCRIPTION;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.MIN_COVERAGE_PERCENTAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.SERVICE_NAME;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Property.property;
import static java.lang.String.format;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.UNNECESSARY;
import static java.time.Duration.ZERO;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JUnit XML report generator, based on <a href="https://github.com/testmoapp/junitxml">Common JUnit XML Format</a>.
 */
@Component
public class JUnitReporter {

  private static final DurationFormatter durationFormatter =
    new DurationFormatter();

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  @RealizesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
  public TestSuites transformToJUnitTestSuites(
    QualityGateReport qualityGateReport
  ) {
    return new TestSuitesFactory().buildForQualityGateReport(qualityGateReport);
  }

  private static class TestSuitesFactory {

    public TestSuites buildForQualityGateReport(
      QualityGateReport qualityGateReport
    ) {
      // Read once, from the report rather than the live gate, so that every test
      // case in one document is judged against the number the report was scored
      // under.
      var qualityGateName = qualityGateReport.getQualityGateConfigName();
      int minCoveragePercentage = qualityGateReport.getMinCoveragePercentage();

      var junitReport = TestSuites.builder()
        .name(qualityGateReport.getQualityGateConfigName())
        .timestamp(qualityGateReport.getCreatedAt().toString())
        .properties(
          Set.of(
            property(
              CALCULATION_ID,
              qualityGateReport.getCalculationId().toString()
            ),
            property(
              MIN_COVERAGE_PERCENTAGE,
              Integer.toString(minCoveragePercentage)
            )
          )
            .stream()
            .sorted(comparing(Property::getName))
            .collect(toCollection(LinkedHashSet::new))
        )
        .build();

      if (!isEmpty(qualityGateReport.getApiTests())) {
        var testSuites = qualityGateReport
          .getApiTests()
          .parallelStream()
          .map(
            new TestSuiteFactory(
              qualityGateName,
              minCoveragePercentage
            )::buildForApiTest
          )
          .sorted(comparing(TestSuite::getName))
          .collect(toCollection(LinkedHashSet::new));
        junitReport.addAllTestSuite(testSuites);
      }

      return withAggregatedStatistics(junitReport);
    }

    private TestSuites withAggregatedStatistics(TestSuites testSuites) {
      AtomicReference<Duration> totalTime = new AtomicReference<>(ZERO);
      testSuites
        .getContainedSuites()
        .forEach(testSuite ->
          totalTime.getAndUpdate(current ->
            current.plus(testSuite.getDuration())
          )
        );

      return testSuites
        .withTests(
          testSuites
            .getContainedSuites()
            .parallelStream()
            .mapToLong(TestSuite::getTests)
            .sum()
        )
        .withAssertions(
          testSuites
            .getContainedSuites()
            .parallelStream()
            .mapToLong(TestSuite::getAssertions)
            .sum()
        )
        .withFailures(
          testSuites
            .getContainedSuites()
            .parallelStream()
            .mapToLong(TestSuite::getFailures)
            .sum()
        )
        .withSkipped(
          testSuites
            .getContainedSuites()
            .parallelStream()
            .mapToLong(TestSuite::getSkipped)
            .sum()
        )
        .withDuration(totalTime.get(), durationFormatter);
    }
  }

  @RequiredArgsConstructor
  private static class TestSuiteFactory {

    private final String qualityGateName;
    private final int minCoveragePercentage;

    public TestSuite buildForApiTest(ApiTest apiTest) {
      var suiteName = constructName(apiTest);
      var testSuite = TestSuite.builder()
        .name(suiteName)
        .properties(
          Set.of(
            property(SERVICE_NAME, apiTest.getServiceName()),
            property(API_NAME, apiTest.getApiName()),
            property(API_VERSION, apiTest.getApiVersion())
          )
            .stream()
            .sorted(comparing(Property::getName))
            .collect(toCollection(LinkedHashSet::new))
        )
        .build();

      Set<TestCase> testCases = apiTest
        .getApiTestResults()
        .parallelStream()
        .map(apiTestResult ->
          new TestCaseFactory(
            qualityGateName,
            minCoveragePercentage
          ).buildForApiTestResult(suiteName, apiTestResult)
        )
        .sorted(comparing(TestCase::getName))
        .collect(toCollection(LinkedHashSet::new));

      testSuite.addAllTestCases(testCases);

      return withAggregatedStatistics(testSuite);
    }

    private String constructName(ApiTest apiTest) {
      return (
        apiTest.getServiceName() +
        ": " +
        apiTest.getApiName() +
        " " +
        apiTest.getApiVersion() +
        " [" +
        apiTest.getApiType().name() +
        "]"
      );
    }

    private TestSuite withAggregatedStatistics(TestSuite testSuite) {
      long tests = testSuite.getTestCases().size();
      long skipped = testSuite
        .getTestCases()
        .parallelStream()
        .filter(testCase -> nonNull(testCase.getSkipped()))
        .count();

      AtomicReference<Duration> totalTime = new AtomicReference<>(ZERO);
      testSuite
        .getTestCases()
        .forEach(testCase ->
          totalTime.getAndUpdate(current ->
            current.plus(testCase.getDuration())
          )
        );

      return testSuite
        .withTests(tests)
        .withAssertions(tests - skipped)
        .withFailures(
          testSuite
            .getTestCases()
            .parallelStream()
            .filter(testCase -> nonNull(testCase.getFailure()))
            .count()
        )
        .withSkipped(skipped)
        .withDuration(totalTime.get(), durationFormatter);
    }
  }

  @RequiredArgsConstructor
  private static class TestCaseFactory {

    private final String qualityGateName;
    private final int minCoveragePercentage;

    /**
     * A criterion the gate excluded becomes {@code skipped} rather than being omitted; an included
     * criterion below the gate's {@code minCoveragePercentage} becomes a {@code failure}; and one
     * that clears the gate without reaching full coverage passes, carrying the remaining gap as
     * {@code system-out}.
     */
    @RealizesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
    public TestCase buildForApiTestResult(
      String suiteName,
      ApiTestResult apiTestResult
    ) {
      var openApiCriteria = OpenApiCoverageCriteria.valueOf(
        apiTestResult.getApiTestCriteria()
      );

      var testCase = TestCase.builder()
        .name(openApiCriteria.getLabel())
        .classname(suiteName)
        .properties(
          Set.of(property(DESCRIPTION, openApiCriteria.getDescription()))
        )
        .build();

      testCase = testCase.withDuration(
        apiTestResult.getDuration(),
        durationFormatter
      );

      if (!apiTestResult.getIncludedInReport()) {
        return testCase.withSkipped(
          Skipped.builder()
            .message(
              format(
                "Test case is not included in Quality-Gate '%s'",
                qualityGateName
              )
            )
            .build()
        );
      }

      var coverage = apiTestResult.getCoverage();

      if (coverage.compareTo(thresholdAsRatio()) < 0) {
        return testCase.withFailure(
          Failure.builder()
            .type("AssertionError")
            .message(apiTestResult.getAdditionalInformation())
            .build()
        );
      } else if (coverage.compareTo(ONE) < 0) {
        return testCase.withSystemOut(
          buildShortOfFullCoverageComment(apiTestResult, coverage)
        );
      }

      return testCase;
    }

    /**
     * The Common JUnit XML Format has no verdict between pass and fail, so a criterion that clears
     * the gate but is not fully covered passes and says why in {@code system-out}.
     */
    private String buildShortOfFullCoverageComment(
      ApiTestResult apiTestResult,
      BigDecimal coverage
    ) {
      var comment = format(
        "Coverage is %s%%, which meets Quality-Gate '%s' minimum of %d%% but is short of full coverage.",
        asPercentage(coverage),
        qualityGateName,
        minCoveragePercentage
      );

      var additionalInformation = apiTestResult.getAdditionalInformation();

      return hasText(additionalInformation)
        ? comment + " " + additionalInformation
        : comment;
    }

    private BigDecimal thresholdAsRatio() {
      return BigDecimal.valueOf(minCoveragePercentage).divide(
        HUNDRED,
        2,
        UNNECESSARY
      );
    }

    private String asPercentage(BigDecimal coverage) {
      return coverage.multiply(HUNDRED).stripTrailingZeros().toPlainString();
    }
  }
}

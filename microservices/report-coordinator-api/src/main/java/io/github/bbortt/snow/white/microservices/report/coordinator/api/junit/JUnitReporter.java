/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.WAIVED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.API_NAME;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.API_VERSION;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.CALCULATION_ID;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.DESCRIPTION;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.JUDGED_TARGET_COUNT;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.MIN_COVERAGE_PERCENTAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.SERVICE_NAME;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.WAIVED_TARGETS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.WAIVED_TARGET_COUNT;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Property.property;
import static java.lang.String.format;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.UNNECESSARY;
import static java.time.Duration.ZERO;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.RealizesCon;
import clew.traceables.clew.annotation.RealizesSw;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
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

  /**
   * Properties in name order, which is what keeps one report always serialising to one document now
   * that a case's set is assembled conditionally rather than listed.
   */
  private static Set<Property> sortedByName(Collection<Property> properties) {
    return properties
      .stream()
      .sorted(comparing(Property::getName))
      .collect(toCollection(LinkedHashSet::new));
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
          sortedByName(
            buildProperties(qualityGateReport, minCoveragePercentage)
          )
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

    /**
     * The bar the whole document was judged against, and - beside it - how many of its targets a
     * waiver removed from judgement, so a reader sees at the root whether any case below rests on a
     * waiver at all.
     */
    @RealizesSw(
      SwTraceables.SW_044_REPORT_PUBLISHES_JUDGED_AND_WAIVED_TARGET_COUNTS
    )
    private Set<Property> buildProperties(
      QualityGateReport qualityGateReport,
      int minCoveragePercentage
    ) {
      var properties = new HashSet<Property>();

      properties.add(
        property(
          CALCULATION_ID,
          qualityGateReport.getCalculationId().toString()
        )
      );
      properties.add(
        property(
          MIN_COVERAGE_PERCENTAGE,
          Integer.toString(minCoveragePercentage)
        )
      );

      var waivedTargetTotal = waivedTargetTotal(qualityGateReport);

      if (nonNull(waivedTargetTotal)) {
        properties.add(
          property(WAIVED_TARGET_COUNT, Integer.toString(waivedTargetTotal))
        );
      }

      return properties;
    }

    /**
     * How many targets this document's judged results had waived, or {@code null} where not one of
     * them recorded a count.
     * <p>
     * Absent rather than {@code 0}, for the reason the JSON reads omit the key: a report scored
     * before the counts were derived does not know how many of its targets were waived, and a zero
     * on the root would state that none were. A report that does know and had none says {@code 0},
     * which is a different sentence.
     * <p>
     * Excluded criteria are left out of the sum, because this number exists to say whether a case
     * below rests on a waiver: a waiver inside a criterion the gate excluded removed nothing from
     * judgement, and counting it here would attribute a waived target to a document that never
     * judged one.
     */
    private @Nullable Integer waivedTargetTotal(
      QualityGateReport qualityGateReport
    ) {
      if (isEmpty(qualityGateReport.getApiTests())) {
        return null;
      }

      var counts = qualityGateReport
        .getApiTests()
        .stream()
        .flatMap(apiTest -> apiTest.getApiTestResults().stream())
        .filter(ApiTestResult::getIncludedInReport)
        .map(ApiTestResult::getWaivedTargetCount)
        .filter(Objects::nonNull)
        .toList();

      return counts.isEmpty()
        ? null
        : counts.stream().mapToInt(Integer::intValue).sum();
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
          sortedByName(
            Set.of(
              property(SERVICE_NAME, apiTest.getServiceName()),
              property(API_NAME, apiTest.getApiName()),
              property(API_VERSION, apiTest.getApiVersion())
            )
          )
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
     * <p>
     * An included criterion holding waived targets adds to whichever of those shapes it takes - the
     * counts and the targets as properties, the reasons and expiries in {@code system-out} - so a
     * result at full coverage <em>because</em> targets were waived is never the plain passing case
     * that a genuinely complete one is.
     * <p>
     * The excluded case is left alone deliberately. {@code skipped} means gate exclusion and
     * nothing else, and a waiver is not an exclusion: adding waiver commentary to a case nothing
     * judged would make the one element this format has for "did not run" carry two meanings.
     */
    @RealizesSw(SwTraceables.SW_017_JUNIT_EXPORT_MIRRORS_THE_GATE_VERDICT)
    @RealizesCon(
      ConTraceables.CON_012_CRITERION_EMPTIED_BY_WAIVERS_IS_NEVER_A_COVERAGE_PASS
    )
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

      var waivedTargets = waivedTargetsOf(apiTestResult);

      testCase = testCase.withProperties(
        sortedByName(
          buildProperties(openApiCriteria, apiTestResult, waivedTargets)
        )
      );

      var waiverComment = buildWaivedTargetsComment(waivedTargets);
      var coverage = apiTestResult.getCoverage();

      if (coverage.compareTo(thresholdAsRatio()) < 0) {
        return testCase
          .withFailure(
            Failure.builder()
              .type("AssertionError")
              .message(apiTestResult.getAdditionalInformation())
              .build()
          )
          .withSystemOut(waiverComment);
      } else if (coverage.compareTo(ONE) < 0) {
        return testCase.withSystemOut(
          join(
            buildShortOfFullCoverageComment(apiTestResult, coverage),
            waiverComment
          )
        );
      }

      return testCase.withSystemOut(waiverComment);
    }

    /**
     * The criterion's description, and - where the result recorded them - the two numbers the JSON
     * reads publish beside its ratio plus the targets a waiver took out of it.
     * <p>
     * A count the result does not carry is omitted rather than written as {@code 0}: a result
     * scored before the counts were derived knows neither number, and {@code judgedTargetCount="0"}
     * would state that the criterion judged nothing - the one claim a vacuous full coverage must
     * never make on its own.
     */
    @RealizesSw(
      SwTraceables.SW_044_REPORT_PUBLISHES_JUDGED_AND_WAIVED_TARGET_COUNTS
    )
    private Set<Property> buildProperties(
      OpenApiCoverageCriteria openApiCriteria,
      ApiTestResult apiTestResult,
      List<ApiTestFinding> waivedTargets
    ) {
      var properties = new HashSet<Property>();

      properties.add(property(DESCRIPTION, openApiCriteria.getDescription()));

      var judgedTargetCount = apiTestResult.getJudgedTargetCount();

      if (nonNull(judgedTargetCount)) {
        properties.add(
          property(JUDGED_TARGET_COUNT, Integer.toString(judgedTargetCount))
        );
      }

      var waivedTargetCount = apiTestResult.getWaivedTargetCount();

      if (nonNull(waivedTargetCount)) {
        properties.add(
          property(WAIVED_TARGET_COUNT, Integer.toString(waivedTargetCount))
        );
      }

      if (!waivedTargets.isEmpty()) {
        properties.add(
          property(
            WAIVED_TARGETS,
            waivedTargets
              .stream()
              .map(this::describeTarget)
              .collect(joining(", "))
          )
        );
      }

      return properties;
    }

    /**
     * The result's waived findings in target order, so one report always serialises to one document
     * despite the findings arriving as an unordered set.
     */
    private List<ApiTestFinding> waivedTargetsOf(ApiTestResult apiTestResult) {
      return apiTestResult
        .getFindings()
        .stream()
        .filter(finding -> WAIVED.equals(finding.getStatus()))
        .sorted(comparing(this::describeTarget))
        .toList();
    }

    /**
     * Why this case's waived targets are not judged, or {@code null} where it has none - in which
     * case the caller's shape is left exactly as it was before waivers existed.
     * <p>
     * Named with their reason and expiry rather than merely counted, because the reader this
     * reaches is a person looking at a green build and asking which of its checks were switched
     * off, and by whose argument. The properties beside it carry the same targets for a reader that
     * parses.
     */
    @RealizesCon(
      ConTraceables.CON_012_CRITERION_EMPTIED_BY_WAIVERS_IS_NEVER_A_COVERAGE_PASS
    )
    private @Nullable String buildWaivedTargetsComment(
      List<ApiTestFinding> waivedTargets
    ) {
      if (waivedTargets.isEmpty()) {
        return null;
      }

      return waivedTargets
        .stream()
        .map(finding ->
          format(
            "Waived until %s: %s - %s",
            finding.getWaiverExpiresOn(),
            describeTarget(finding),
            finding.getWaiverReason()
          )
        )
        .collect(joining("\n"));
    }

    /**
     * The target as the waiver that removed it named it - {@code GET /pets/{petId} (404)}, the shape
     * {@code Waiver#target()} writes, so the line a reader finds here matches the entry they wrote.
     * <p>
     * Every waived finding carries a path and a method, because a waiver is matched on both; the
     * one discriminator beyond the operation is parenthesised, and a waiver naming the operation
     * itself has none.
     */
    private String describeTarget(ApiTestFinding finding) {
      var discriminator = Stream.of(
        finding.getResponseCode(),
        finding.getParameterName(),
        finding.getContentType()
      )
        .filter(Objects::nonNull)
        .findFirst()
        .map(" (%s)"::formatted)
        .orElse("");

      return (
        finding.getHttpMethod() + " " + finding.getHttpPath() + discriminator
      );
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

    private String join(String comment, @Nullable String waiverComment) {
      return nonNull(waiverComment) ? comment + "\n" + waiverComment : comment;
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

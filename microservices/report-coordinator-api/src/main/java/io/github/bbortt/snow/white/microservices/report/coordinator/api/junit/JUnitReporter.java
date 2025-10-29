/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.junit;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.API_NAME;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.API_VERSION;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.CALCULATION_ID;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.DESCRIPTION;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Properties.SERVICE_NAME;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.Property.property;
import static java.lang.String.format;
import static java.math.BigDecimal.ONE;
import static java.time.Duration.ZERO;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.util.CollectionUtils.isEmpty;
import static tools.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static tools.jackson.dataformat.xml.XmlMapper.xmlBuilder;
import static tools.jackson.dataformat.xml.XmlWriteFeature.WRITE_STANDALONE_YES_TO_XML_DECLARATION;
import static tools.jackson.dataformat.xml.XmlWriteFeature.WRITE_XML_DECLARATION;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * JUnit XML report generator, based on <a href="https://github.com/testmoapp/junitxml">Common JUnit XML Format</a>.
 */
@Component
public class JUnitReporter {

  private static final DurationFormatter durationFormatter =
    new DurationFormatter();
  private static final XmlMapper xmlMapper = xmlBuilder()
    .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(NON_EMPTY))
    .enable(INDENT_OUTPUT)
    .enable(WRITE_XML_DECLARATION)
    .enable(WRITE_STANDALONE_YES_TO_XML_DECLARATION)
    .build();

  public Resource transformToJUnitReport(QualityGateReport qualityGateReport)
    throws JUnitReportCreationException {
    var testSuites = new TestSuitesFactory().buildForQualityGateReport(
      qualityGateReport
    );

    try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
      xmlMapper.writeValue(byteArrayOutputStream, testSuites);
      return new JUnitReportResource(byteArrayOutputStream.toByteArray());
    } catch (IOException e) {
      throw new JUnitReportCreationException(e);
    }
  }

  private static class TestSuitesFactory {

    public TestSuites buildForQualityGateReport(
      QualityGateReport qualityGateReport
    ) {
      var junitReport = TestSuites.builder()
        .name(qualityGateReport.getQualityGateConfigName())
        .timestamp(qualityGateReport.getCreatedAt().toString())
        .properties(
          Set.of(
            property(
              CALCULATION_ID,
              qualityGateReport.getCalculationId().toString()
            )
          )
        )
        .build();

      if (!isEmpty(qualityGateReport.getApiTests())) {
        var testSuites = qualityGateReport
          .getApiTests()
          .parallelStream()
          .map(new TestSuiteFactory()::buildForApiTest)
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

  private static class TestSuiteFactory {

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
          new TestCaseFactory().buildForApiTestResult(suiteName, apiTestResult)
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

  private static class TestCaseFactory {

    public TestCase buildForApiTestResult(
      String suiteName,
      ApiTestResult apiTestResult
    ) {
      var openApiCriteria = OpenApiCriteria.valueOf(
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
                apiTestResult
                  .getApiTest()
                  .getQualityGateReport()
                  .getQualityGateConfigName()
              )
            )
            .build()
        );
      } else if (apiTestResult.getCoverage().compareTo(ONE) < 0) {
        return testCase.withFailure(
          Failure.builder()
            .type("AssertionError")
            .message(apiTestResult.getAdditionalInformation())
            .build()
        );
      }

      return testCase;
    }
  }
}

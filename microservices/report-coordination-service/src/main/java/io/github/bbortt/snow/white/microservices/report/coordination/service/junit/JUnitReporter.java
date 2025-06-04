/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static java.lang.String.format;
import static java.math.BigDecimal.ONE;
import static java.time.Duration.ZERO;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * JUnit XML report generator, based on <a href="https://github.com/testmoapp/junitxml">Common JUnit XML Format</a>.
 */
@Component
public class JUnitReporter {

  private final Marshaller marshaller;

  public JUnitReporter() throws JAXBException {
    var context = JAXBContext.newInstance(TestSuites.class);
    marshaller = context.createMarshaller();
    marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
  }

  public Resource transformToJUnitReport(QualityGateReport qualityGateReport)
    throws JUnitReportCreationException {
    var testSuites = new TestSuitesFactory()
      .buildForQualityGateReport(qualityGateReport);

    try (var byteArrayOutputStream = new ByteArrayOutputStream()) {
      marshaller.marshal(testSuites, byteArrayOutputStream);
      return new JUnitReportResource(byteArrayOutputStream.toByteArray());
    } catch (IOException | JAXBException e) {
      throw new JUnitReportCreationException(e);
    }
  }

  private static class TestSuitesFactory {

    private final AtomicLong tests = new AtomicLong(0);
    private final AtomicLong failures = new AtomicLong(0);
    private final AtomicReference<Duration> time = new AtomicReference<>(ZERO);

    public TestSuites buildForQualityGateReport(
      QualityGateReport qualityGateReport
    ) {
      var testSuites = TestSuites.builder()
        .name(qualityGateReport.getQualityGateConfigName())
        .timestamp(qualityGateReport.getCreatedAt().toString())
        .build();

      if (nonNull(qualityGateReport.getOpenApiTestResults())) {
        var openApiTestSuite = TestSuite.builder()
          .name("OpenAPI Specification")
          .build();

        qualityGateReport
          .getOpenApiTestResults()
          .stream()
          .sorted(comparing(OpenApiTestResult::getOpenApiTestCriteria))
          .map(this::generateOpenApiTestCase)
          .forEach(openApiTestSuite::addTestCase);

        testSuites.addTestSuite(openApiTestSuite);
      }

      return testSuites
        .withTests(tests.get())
        .withFailures(failures.get())
        .withTime(toSecondsWithPrecision(time.get()));
    }

    private TestCase generateOpenApiTestCase(
      OpenApiTestResult openApiTestResult
    ) {
      var openApiCriteria = OpenApiCriteria.valueOf(
        openApiTestResult.getOpenApiTestCriteria()
      );

      var testCase = TestCase.builder()
        .name(openApiCriteria.getLabel())
        .classname(openApiCriteria.name())
        .time(toSecondsWithPrecision(openApiTestResult.getDuration()))
        .build();

      tests.getAndIncrement();
      time.getAndUpdate(current ->
        current.plus(openApiTestResult.getDuration())
      );

      if (openApiTestResult.getCoverage().compareTo(ONE) < 0) {
        failures.getAndIncrement();

        return testCase.withFailure(
          Failure.builder()
            .type("coverage")
            .message(openApiTestResult.getAdditionalInformation())
            .build()
        );
      }

      return testCase;
    }

    private static String toSecondsWithPrecision(Duration duration) {
      double seconds = duration.toNanos() / 1_000_000_000.0;

      if (seconds == 0) {
        return "0";
      }

      return format("%.5f", seconds);
    }
  }
}

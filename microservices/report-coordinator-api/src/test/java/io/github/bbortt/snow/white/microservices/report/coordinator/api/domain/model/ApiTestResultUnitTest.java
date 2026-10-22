/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApiTestResultUnitTest {

  private ApiTest.ApiTestBuilder apiTestBuilder;

  @BeforeEach
  void beforeEachSetup() {
    apiTestBuilder = ApiTest.builder()
      .serviceName("foo")
      .apiName("bar")
      .apiVersion("baz")
      .apiType(OPENAPI.getVal());
  }

  @Nested
  class GetReportStatusTest {

    @EnumSource
    @ParameterizedTest
    void shouldTransformShortToEnumValue(ReportStatus reportStatus) {
      var fixture = apiTestBuilder.reportStatus(reportStatus.getVal()).build();

      assertThat(fixture.getReportStatus()).isEqualTo(reportStatus);
    }
  }

  @Nested
  class WithReportStatusTest {

    @EnumSource
    @ParameterizedTest
    void shouldAppendReportStatus(ReportStatus reportStatus) {
      assertThat(apiTestBuilder.build().withReportStatus(reportStatus))
        .extracting(ApiTest::getReportStatus)
        .isEqualTo(reportStatus);
    }
  }

  @Nested
  class EqualsAndHashCodeTest {

    @Test
    @VerifiesSw(
      SwTraceables.SW_020_REDELIVERED_CRITERION_RESULT_REPLACES_EXISTING_ONE
    )
    void shouldBeEqualAndHashIdentically_whenApiTestCriteriaAndApiTestMatch() {
      var apiTest = apiTestBuilder.build();

      var first = ApiTestResult.builder()
        .apiTestCriteria("PATH_COVERAGE")
        .coverage(ZERO)
        .includedInReport(FALSE)
        .duration(Duration.ofSeconds(1))
        .apiTest(apiTest)
        .build();

      var second = ApiTestResult.builder()
        .apiTestCriteria("PATH_COVERAGE")
        .coverage(ONE)
        .includedInReport(TRUE)
        .duration(Duration.ofSeconds(2))
        .apiTest(apiTest)
        .build();

      assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_020_REDELIVERED_CRITERION_RESULT_REPLACES_EXISTING_ONE
    )
    void shouldNotBeEqual_whenApiTestCriteriaDiffers() {
      var apiTest = apiTestBuilder.build();

      var first = ApiTestResult.builder()
        .apiTestCriteria("PATH_COVERAGE")
        .coverage(ONE)
        .includedInReport(TRUE)
        .duration(Duration.ofSeconds(1))
        .apiTest(apiTest)
        .build();

      var second = ApiTestResult.builder()
        .apiTestCriteria("HTTP_METHOD_COVERAGE")
        .coverage(ONE)
        .includedInReport(TRUE)
        .duration(Duration.ofSeconds(1))
        .apiTest(apiTest)
        .build();

      assertThat(first).isNotEqualTo(second);
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_020_REDELIVERED_CRITERION_RESULT_REPLACES_EXISTING_ONE
    )
    void shouldNotBeEqual_whenApiTestDiffers() {
      var first = ApiTestResult.builder()
        .apiTestCriteria("PATH_COVERAGE")
        .coverage(ONE)
        .includedInReport(TRUE)
        .duration(Duration.ofSeconds(1))
        .apiTest(apiTestBuilder.build())
        .build();

      var second = ApiTestResult.builder()
        .apiTestCriteria("PATH_COVERAGE")
        .coverage(ONE)
        .includedInReport(TRUE)
        .duration(Duration.ofSeconds(1))
        .apiTest(apiTestBuilder.build())
        .build();

      assertThat(first).isNotEqualTo(second);
    }
  }
}

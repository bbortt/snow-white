/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ApiTestResultTest {

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
}

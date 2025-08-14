/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class QualityGateReportMapperTest {

  @Nested
  class ToStatusEnum {

    public static Stream<ReportStatus> shouldReturnMappedEnumValue() {
      return Stream.of(FAILED, PASSED);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnMappedEnumValue(ReportStatus reportStatus) {
      assertThat(
        new QualityGateReportMapperImpl().toStatusEnum(reportStatus)
      ).isEqualTo(
        CalculateQualityGate202Response.StatusEnum.valueOf(reportStatus.name())
      );
    }

    public static Stream<ReportStatus> shouldReturnInProgress() {
      return Stream.of(NOT_STARTED, IN_PROGRESS);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnInProgress(ReportStatus reportStatus) {
      assertThat(
        new QualityGateReportMapperImpl().toStatusEnum(reportStatus)
      ).isEqualTo(CalculateQualityGate202Response.StatusEnum.IN_PROGRESS);
    }
  }

  @Nested
  class ToListStatusEnum {

    public static Stream<ReportStatus> shouldReturnMappedEnumValue() {
      return Stream.of(FAILED, PASSED);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnMappedEnumValue(ReportStatus reportStatus) {
      assertThat(
        new QualityGateReportMapperImpl().toListStatusEnum(reportStatus)
      ).isEqualTo(
        ListQualityGateReports200ResponseInner.StatusEnum.valueOf(
          reportStatus.name()
        )
      );
    }

    public static Stream<ReportStatus> shouldReturnInProgress() {
      return Stream.of(NOT_STARTED, IN_PROGRESS);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnInProgress(ReportStatus reportStatus) {
      assertThat(
        new QualityGateReportMapperImpl().toListStatusEnum(reportStatus)
      ).isEqualTo(
        ListQualityGateReports200ResponseInner.StatusEnum.IN_PROGRESS
      );
    }
  }
}

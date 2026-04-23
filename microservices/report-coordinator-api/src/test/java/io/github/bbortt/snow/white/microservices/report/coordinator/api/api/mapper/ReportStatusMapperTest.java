/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202ResponseInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInnerInterfacesInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ReportStatusMapperTest {

  private ReportStatusMapper fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ReportStatusMapperImpl();
  }

  @Nested
  class ToResponseStatusEnumTest {

    public static Stream<ReportStatus> shouldReturnMappedEnumValue() {
      return Stream.of(FAILED, PASSED);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnMappedEnumValue(ReportStatus reportStatus) {
      assertThat(fixture.toResponseStatusEnum(reportStatus)).isEqualTo(
        CalculateQualityGate202Response.StatusEnum.valueOf(reportStatus.name())
      );
    }

    public static Stream<ReportStatus> shouldReturnInProgress() {
      return Stream.of(NOT_STARTED, IN_PROGRESS);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnInProgress(ReportStatus reportStatus) {
      assertThat(fixture.toResponseStatusEnum(reportStatus)).isEqualTo(
        CalculateQualityGate202Response.StatusEnum.IN_PROGRESS
      );
    }
  }

  @Nested
  class ToResponseInnerStatusEnumTest {

    public static Stream<ReportStatus> shouldReturnMappedEnumValue() {
      return Stream.of(FAILED, PASSED);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnMappedEnumValue(ReportStatus reportStatus) {
      assertThat(fixture.toResponseInnerStatusEnum(reportStatus)).isEqualTo(
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
      assertThat(fixture.toResponseInnerStatusEnum(reportStatus)).isEqualTo(
        ListQualityGateReports200ResponseInner.StatusEnum.IN_PROGRESS
      );
    }
  }

  @Nested
  class ToResponseInterfacesInnerStatusEnumTest {

    public static Stream<ReportStatus> shouldReturnMappedEnumValue() {
      return Stream.of(FAILED, PASSED);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnMappedEnumValue(ReportStatus reportStatus) {
      assertThat(
        fixture.toResponseInterfacesInnerStatusEnum(reportStatus)
      ).isEqualTo(
        CalculateQualityGate202ResponseInterfacesInner.StatusEnum.valueOf(
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
        fixture.toResponseInterfacesInnerStatusEnum(reportStatus)
      ).isEqualTo(
        CalculateQualityGate202ResponseInterfacesInner.StatusEnum.IN_PROGRESS
      );
    }
  }

  @Nested
  class ToResponseInnerInterfacesInnerStatusEnumTest {

    public static Stream<ReportStatus> shouldReturnMappedEnumValue() {
      return Stream.of(FAILED, PASSED);
    }

    @MethodSource
    @ParameterizedTest
    void shouldReturnMappedEnumValue(ReportStatus reportStatus) {
      assertThat(
        fixture.toResponseInnerInterfacesInnerStatusEnum(reportStatus)
      ).isEqualTo(
        ListQualityGateReports200ResponseInnerInterfacesInner.StatusEnum.valueOf(
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
        fixture.toResponseInnerInterfacesInnerStatusEnum(reportStatus)
      ).isEqualTo(
        ListQualityGateReports200ResponseInnerInterfacesInner.StatusEnum.IN_PROGRESS
      );
    }
  }
}

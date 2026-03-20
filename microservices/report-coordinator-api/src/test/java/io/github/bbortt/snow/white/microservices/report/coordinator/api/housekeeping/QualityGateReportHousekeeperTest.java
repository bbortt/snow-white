/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.housekeeping;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.TIMED_OUT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class QualityGateReportHousekeeperTest {

  private static final Instant FIXED_NOW = Instant.parse(
    "2026-03-18T12:00:00Z"
  );

  @Mock
  private Clock clockMock;

  @Mock
  private QualityGateReportRepository qualityGateReportRepositoryMock;

  private ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  private QualityGateReportHousekeeper fixture;

  @BeforeEach
  void beforeEachSetup() {
    reportCoordinationServiceProperties =
      new ReportCoordinationServiceProperties();

    fixture = new QualityGateReportHousekeeper(
      clockMock,
      qualityGateReportRepositoryMock,
      reportCoordinationServiceProperties
    );
  }

  @Nested
  class RunTest {

    @BeforeEach
    void beforeEachSetup() {
      when(clockMock.instant()).thenReturn(FIXED_NOW);
    }

    @Test
    void timesOutReportsCreatedMoreThanFiveMinutesAgo_byDefault() {
      fixture.run();

      verify(
        qualityGateReportRepositoryMock
      ).updateStatusToTimedOutByCreatedAtBefore(
        Instant.parse("2026-03-18T11:55:00Z"),
        TIMED_OUT.getVal(),
        Set.of(NOT_STARTED.getVal(), IN_PROGRESS.getVal())
      );
    }

    @Test
    void timesOutReportsCreatedMoreThanTenMinutesAgo() {
      reportCoordinationServiceProperties
        .getHousekeepingProperties()
        .setCutoffSeconds(600L);

      fixture.run();

      verify(
        qualityGateReportRepositoryMock
      ).updateStatusToTimedOutByCreatedAtBefore(
        Instant.parse("2026-03-18T11:50:00Z"),
        TIMED_OUT.getVal(),
        Set.of(NOT_STARTED.getVal(), IN_PROGRESS.getVal())
      );
    }
  }
}

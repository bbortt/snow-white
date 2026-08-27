/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.housekeeping;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.TIMED_OUT;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class QualityGateReportHousekeeper implements HousekeepingJob {

  private final Clock clock;
  private final QualityGateReportRepository qualityGateReportRepository;
  private final ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  @Override
  @Transactional
  public void run() {
    var cutoff = Instant.now(clock).minusSeconds(
      reportCoordinationServiceProperties
        .getHousekeepingProperties()
        .getCutoffSeconds()
    );
    logger.debug("Deleting QualityGate reports created before {}", cutoff);

    int updated =
      qualityGateReportRepository.updateStatusToTimedOutByCreatedAtBefore(
        cutoff,
        TIMED_OUT.getVal(),
        Set.of(NOT_STARTED.getVal(), IN_PROGRESS.getVal())
      );

    logger.info("Timed out {} stale QualityGate report(s)", updated);
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.TIMED_OUT;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.Main;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@Isolated
@DirtiesContext
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.report.coordinator.api.calculation-request-topic=calculation-request-topic",
    "snow.white.report.coordinator.api.housekeeping.cron=* * * * * *\n",
    "snow.white.report.coordinator.api.init-topics=true",
    "snow.white.report.coordinator.api.openapi-calculation-response.topic=openapi-calculation-response-topic",
    "snow.white.report.coordinator.api.public-api-gateway-url=http://localhost:9080",
  }
)
public class HousekeepingServiceIT extends AbstractReportCoordinationServiceIT {

  private static final UUID CALCULATION_UUID = UUID.fromString(
    "f2e2256b-307a-458a-9a40-5f143b84f299"
  );

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  public static Stream<ReportStatus> postRequest_shouldTriggerHousekeeping() {
    return Stream.of(NOT_STARTED, IN_PROGRESS);
  }

  @BeforeEach
  void beforeEachSetup() {
    qualityGateReportRepository.deleteAll();
  }

  @MethodSource
  @ParameterizedTest
  void postRequest_shouldTriggerHousekeeping(ReportStatus reportStatus) {
    var qualityGateReport = qualityGateReportRepository.saveAndFlush(
      QualityGateReport.builder()
        .calculationId(CALCULATION_UUID)
        .reportParameter(
          ReportParameter.builder().calculationId(CALCULATION_UUID).build()
        )
        .qualityGateConfigName("qualityGateConfigName")
        .reportStatus(reportStatus.getVal())
        .createdAt(Instant.now().minusSeconds(301L))
        .build()
    );

    stream(ReportStatus.values())
      .filter(s -> !List.of(NOT_STARTED, IN_PROGRESS, TIMED_OUT).contains(s))
      .forEach(ignoredReportStatus -> {
        var calculationId = UUID.randomUUID();
        qualityGateReportRepository.saveAndFlush(
          QualityGateReport.builder()
            .calculationId(calculationId)
            .reportParameter(
              ReportParameter.builder().calculationId(calculationId).build()
            )
            .qualityGateConfigName("qualityGateConfigName")
            .reportStatus(ignoredReportStatus.getVal())
            .createdAt(Instant.now().minusSeconds(301L))
            .build()
        );
      });

    await()
      .atMost(2, SECONDS)
      .untilAsserted(() ->
        assertThat(
          qualityGateReportRepository.findById(
            qualityGateReport.getCalculationId()
          )
        )
          .isPresent()
          .get()
          .extracting(QualityGateReport::getReportStatus)
          .isEqualTo(TIMED_OUT)
      );

    var allQualityGateReports = qualityGateReportRepository
      .findAll()
      .stream()
      .filter(r -> !r.getCalculationId().equals(CALCULATION_UUID))
      .toList();

    assertThat(allQualityGateReports)
      .isNotEmpty()
      .allSatisfy(r -> assertThat(r.getReportStatus()).isNotEqualTo(TIMED_OUT));
  }
}

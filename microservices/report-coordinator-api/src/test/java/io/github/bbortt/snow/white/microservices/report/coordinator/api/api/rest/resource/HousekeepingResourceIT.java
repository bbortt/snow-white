/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.HousekeepingApi.PATH_HOUSEKEEPING;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.TIMED_OUT;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class HousekeepingResourceIT extends AbstractReportCoordinationServiceIT {

  private static final UUID CALCULATION_UUID = UUID.fromString(
    "7cdd7ec4-4c6b-4339-a3a6-50cdcea7db4b"
  );

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void postRequest_shouldTriggerHousekeeping() throws Exception {
    var qualityGateReport = qualityGateReportRepository.saveAndFlush(
      QualityGateReport.builder()
        .calculationId(CALCULATION_UUID)
        .reportParameter(
          ReportParameter.builder().calculationId(CALCULATION_UUID).build()
        )
        .qualityGateConfigName("qualityGateConfigName")
        .createdAt(Instant.now().minusSeconds(301L))
        .build()
    );

    mockMvc.perform(post(PATH_HOUSEKEEPING)).andExpect(status().isAccepted());

    await()
      .atMost(1, MINUTES)
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
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestResultForUnknownApiExceptionTest {

  @Test
  void constructorAddsMessage() {
    var qualityGateReport = QualityGateReport.builder()
      .calculationId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
      .reportParameter(mock(ReportParameter.class))
      .build();

    var apiInformation = ApiInformation.builder()
      .serviceName("test")
      .apiName("test")
      .apiVersion("v1")
      .build();

    assertThat(
      new TestResultForUnknownApiException(qualityGateReport, apiInformation)
    ).hasMessage(
      "Test result for API serviceName=test apiName=test apiVersion=v1 not found in Quality-Gate report with calculation id '123e4567-e89b-12d3-a456-426614174000'"
    );
  }
}

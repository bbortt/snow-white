/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static java.util.Collections.emptySet;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import java.util.UUID;

public final class TestData {

  public static ApiInformation defaultApiInformation() {
    return ApiInformation.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .apiType(OPENAPI)
      .build();
  }

  public static ApiTest defaultApiTest() {
    return ApiTest.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .apiType(OPENAPI.getVal())
      .build();
  }

  public static QualityGateReport minimalQualityGateReport(UUID calculationId) {
    return QualityGateReport.builder()
      .calculationId(calculationId)
      .apiTests(emptySet())
      .reportParameter(mock(ReportParameter.class))
      .build();
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.UNSPECIFIED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class ApiTestResultIT extends AbstractReportCoordinationServiceIT {

  public static final UUID CALCULATION_ID = UUID.fromString(
    "ebc60205-66b5-495a-9d56-8b9003302ad2"
  );

  @Autowired
  private ApiTestRepository apiTestRepository;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @AfterEach
  void afterEachTeardown() {
    qualityGateReportRepository.deleteById(CALCULATION_ID);
  }

  @ParameterizedTest
  @EnumSource(OpenApiCriteria.class)
  void shouldBePersisted(OpenApiCriteria openApiCriteria) {
    var qualityGateReport = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName("qualityGateConfigName")
        .reportParameter(
          ReportParameter.builder().calculationId(CALCULATION_ID).build()
        )
        .reportStatus(PASSED.getVal())
        .build()
    );

    var apiTest = ApiTest.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .apiType(UNSPECIFIED.getVal())
      .qualityGateReport(qualityGateReport)
      .build();

    apiTest = apiTestRepository.save(apiTest);

    apiTest = apiTest.withApiTestResults(
      Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(openApiCriteria.name())
          .coverage(ZERO)
          .includedInReport(TRUE)
          .duration(Duration.ofSeconds(1))
          .apiTest(apiTest)
          .build()
      )
    );

    final var finalApiTest = apiTest;
    assertThatCode(() ->
      apiTestRepository.save(finalApiTest)
    ).doesNotThrowAnyException();
  }
}

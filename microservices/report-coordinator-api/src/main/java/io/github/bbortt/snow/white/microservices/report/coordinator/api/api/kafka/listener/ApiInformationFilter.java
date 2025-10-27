/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static java.util.Objects.isNull;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.TestResultForUnknownApiException;

final class ApiInformationFilter {

  ApiTest findApiTestMatchingApiInformationInQualityGateReport(
    QualityGateReport qualityGateReport,
    ApiInformation apiInformation
  ) {
    return qualityGateReport
      .getApiTests()
      .parallelStream()
      .filter(
        apiTest ->
          apiTest.getServiceName().equals(apiInformation.getServiceName()) &&
          apiTest.getApiName().equals(apiInformation.getApiName()) &&
          (isNull(apiTest.getApiVersion()) ||
            apiTest.getApiVersion().equals(apiInformation.getApiVersion()))
      )
      .findFirst()
      .orElseThrow(() ->
        new TestResultForUnknownApiException(qualityGateReport, apiInformation)
      );
  }
}

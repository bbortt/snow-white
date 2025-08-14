/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception;

import static java.lang.String.format;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;

public class TestResultForUnknownApiException extends RuntimeException {

  public TestResultForUnknownApiException(
    QualityGateReport qualityGateReport,
    ApiInformation apiInformation
  ) {
    super(
      format(
        "Test result for API serviceName=%s apiName=%s apiVersion=%s not found in Quality-Gate report with calculation id '%s'",
        apiInformation.getServiceName(),
        apiInformation.getApiName(),
        apiInformation.getApiVersion(),
        qualityGateReport.getCalculationId()
      )
    );
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.mapper.ApiTestMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.mapper.ReportParameterMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate400Response;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final ReportService reportService;

  private final ApiTestMapper apiTestMapper;
  private final ReportParameterMapper reportParameterMapper;
  private final QualityGateReportMapper qualityGateReportMapper;

  private final ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  @Override
  public ResponseEntity calculateQualityGate(
    String qualityGateConfigName,
    CalculateQualityGateRequest calculateQualityGateRequest
  ) {
    try {
      var apiTests = apiTestMapper.getApiTests(calculateQualityGateRequest);
      var reportParameter = reportParameterMapper.fromDto(
        calculateQualityGateRequest
      );
      var report = reportService.initializeQualityGateCalculation(
        qualityGateConfigName,
        apiTests,
        reportParameter
      );

      return ResponseEntity.status(ACCEPTED)
        .header(
          LOCATION,
          format(
            "%s/quality-gate/%s",
            reportCoordinationServiceProperties.getPublicApiGatewayUrl(),
            report.getCalculationId()
          )
        )
        .body(qualityGateReportMapper.toDto(report));
    } catch (QualityGateNotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(
        CalculateQualityGate400Response.builder()
          .code(NOT_FOUND.getReasonPhrase())
          .message(
            format(
              "Quality-Gate configuration '%s' does not exist!",
              qualityGateConfigName
            )
          )
          .build()
      );
    }
  }
}

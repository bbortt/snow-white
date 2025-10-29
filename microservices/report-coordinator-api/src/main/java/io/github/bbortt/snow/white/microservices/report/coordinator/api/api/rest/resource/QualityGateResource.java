/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ReportParameterMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate400Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import java.util.UUID;
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
        calculateQualityGateRequest,
        UUID.randomUUID()
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
    } catch (QualityGateNotFoundException _) {
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

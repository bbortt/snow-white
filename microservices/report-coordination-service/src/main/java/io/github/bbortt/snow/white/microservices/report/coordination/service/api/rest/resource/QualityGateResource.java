/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate400Response;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.ReportParameterMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final ReportService reportService;
  private final QualityGateReportMapper qualityGateReportMapper;
  private final ReportParameterMapper reportParameterMapper;

  @Override
  public ResponseEntity calculateQualityGate(
    String qualityGateConfigName,
    CalculateQualityGateRequest calculateQualityGateRequest
  ) {
    try {
      var report = reportService.initializeQualityGateCalculation(
        qualityGateConfigName,
        reportParameterMapper.fromDto(calculateQualityGateRequest)
      );

      return ResponseEntity.status(ACCEPTED).body(
        qualityGateReportMapper.toCalculateQualityGateResponse(report)
      );
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

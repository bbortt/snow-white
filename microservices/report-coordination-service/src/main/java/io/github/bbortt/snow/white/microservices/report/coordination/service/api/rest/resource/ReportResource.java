/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.ReportApi;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportResource implements ReportApi {

  private final ReportService reportService;
  private final QualityGateReportMapper qualityGateReportMapper;

  @Override
  public ResponseEntity getReportByCalculationId(UUID calculationId) {
    var optionalReport = reportService.findReportByCalculationId(calculationId);

    if (optionalReport.isEmpty()) {
      return ResponseEntity.status(NOT_FOUND).body(
        Error.builder()
          .code(NOT_FOUND.getReasonPhrase())
          .message(format("No report by id '%s' exists!", calculationId))
          .build()
      );
    }

    var report = optionalReport.get();

    if (IN_PROGRESS.equals(report.getReportStatus())) {
      return ResponseEntity.status(ACCEPTED).body(
        qualityGateReportMapper.toDto(report)
      );
    }

    return ResponseEntity.ok(qualityGateReportMapper.toDto(report));
  }
}

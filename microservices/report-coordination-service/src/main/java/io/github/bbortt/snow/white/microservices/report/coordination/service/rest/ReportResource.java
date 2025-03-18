package io.github.bbortt.snow.white.microservices.report.coordination.service.rest;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.Report;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.QualityGate;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportResource implements ReportApi {

  private final ConversionService conversionService;
  private final ReportService reportService;

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

    if (isNull(report.getOpenApiCoverage())) {
      return ResponseEntity.status(ACCEPTED).body(convertToDto(report));
    }

    return ResponseEntity.ok(convertToDto(report));
  }

  private QualityGate convertToDto(Report report) {
    return conversionService.convert(report, QualityGate.class);
  }
}

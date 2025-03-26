package io.github.bbortt.snow.white.microservices.report.coordination.service.rest;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.mapper.ReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.mapper.ReportParameterMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.QualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateNotFoundException;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final ReportService reportService;
  private final ReportMapper reportMapper;
  private final ReportParameterMapper reportParameterMapper;

  @Override
  public ResponseEntity calculateQualityGate(
    String qualityGateConfigName,
    QualityGateRequest qualityGateRequest
  ) {
    try {
      var report = reportService.initializeQualityGateCalculation(
        qualityGateConfigName,
        reportParameterMapper.fromDto(qualityGateRequest)
      );

      return ResponseEntity.status(ACCEPTED).body(reportMapper.toDto(report));
    } catch (QualityGateNotFoundException e) {
      return ResponseEntity.status(NOT_FOUND).body(
        Error.builder()
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

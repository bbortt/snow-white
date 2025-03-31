package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.QualityGateApi;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateCalculationRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.ReportMapper;
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
  private final ReportMapper reportMapper;
  private final ReportParameterMapper reportParameterMapper;

  @Override
  public ResponseEntity calculateQualityGate(
    String qualityGateConfigName,
    QualityGateCalculationRequest qualityGateCalculationRequest
  ) {
    try {
      var report = reportService.initializeQualityGateCalculation(
        qualityGateConfigName,
        reportParameterMapper.fromDto(qualityGateCalculationRequest)
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

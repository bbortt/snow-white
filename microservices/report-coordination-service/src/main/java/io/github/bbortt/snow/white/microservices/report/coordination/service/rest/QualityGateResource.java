package io.github.bbortt.snow.white.microservices.report.coordination.service.rest;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.rest.QualityGateMapper.toDto;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.QualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final ReportService reportService;
  private final QualityGateService qualityGateService;

  @Override
  public ResponseEntity calculateQualityGate(
    String qualityGateName,
    QualityGateRequest qualityGateRequest
  ) {
    if (!qualityGateService.existsByName(qualityGateName)) {
      return ResponseEntity.status(NOT_FOUND).body(
        Error.builder()
          .code(NOT_FOUND.getReasonPhrase())
          .message(
            format(
              "Quality-Gate configuration '%s' does not exist!",
              qualityGateName
            )
          )
          .build()
      );
    }

    var qualityGateReport = reportService.createInitialReport(
      qualityGateName,
      qualityGateRequest
    );
    reportService.dispatchCalculations(qualityGateReport);

    return ResponseEntity.status(ACCEPTED).body(toDto(qualityGateReport));
  }
}

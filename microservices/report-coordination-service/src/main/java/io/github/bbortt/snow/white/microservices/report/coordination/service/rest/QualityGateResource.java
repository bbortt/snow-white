package io.github.bbortt.snow.white.microservices.report.coordination.service.rest;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.QualityGate;
import io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.QualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateNotFoundException;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class QualityGateResource implements QualityGateApi {

  private final ConversionService conversionService;
  private final ReportService reportService;
  private final QualityGateService qualityGateService;

  @Override
  public ResponseEntity calculateQualityGate(
    String qualityGateConfigName,
    QualityGateRequest qualityGateRequest
  ) {
    try {
      var qualityGateReport = reportService.initializeQualityGateCalculation(
        qualityGateConfigName,
        conversionService.convert(qualityGateRequest, ReportParameters.class)
      );

      return ResponseEntity.status(ACCEPTED).body(
        conversionService.convert(qualityGateReport, QualityGate.class)
      );
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

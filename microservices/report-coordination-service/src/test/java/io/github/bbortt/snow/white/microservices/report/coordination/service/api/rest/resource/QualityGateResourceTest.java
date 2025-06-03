/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate400Response;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.ReportParameterMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class QualityGateResourceTest {

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private QualityGateReportMapper qualityGateReportMapperMock;

  @Mock
  private ReportParameterMapper reportParameterMapperMock;

  private QualityGateResource fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateResource(
      reportServiceMock,
      qualityGateReportMapperMock,
      reportParameterMapperMock
    );
  }

  @Nested
  class CalculateQualityGate {

    private static final String QUALITY_GATE_CONFIG_NAME =
      "qualityGateConfigName";

    @Mock
    private CalculateQualityGateRequest qualityGateCalculationRequestMock;

    @Test
    void shouldInitializeQualityGateCalculation()
      throws QualityGateNotFoundException {
      var reportParameters = mock(ReportParameters.class);
      doReturn(reportParameters)
        .when(reportParameterMapperMock)
        .fromDto(qualityGateCalculationRequestMock);

      var qualityGateReport = mock(QualityGateReport.class);
      doReturn(qualityGateReport)
        .when(reportServiceMock)
        .initializeQualityGateCalculation(
          QUALITY_GATE_CONFIG_NAME,
          reportParameters
        );

      var responseDto = mock(CalculateQualityGate202Response.class);
      doReturn(responseDto)
        .when(qualityGateReportMapperMock)
        .toCalculateQualityGateResponse(qualityGateReport);

      var response = fixture.calculateQualityGate(
        QUALITY_GATE_CONFIG_NAME,
        qualityGateCalculationRequestMock
      );

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(ACCEPTED),
          r -> assertThat(r.getBody()).isEqualTo(responseDto)
        );
    }

    @Test
    void shouldReturnNotFoundResponse_whenConfigurationDoesNotExists()
      throws QualityGateNotFoundException {
      var reportParameters = mock(ReportParameters.class);
      doReturn(reportParameters)
        .when(reportParameterMapperMock)
        .fromDto(qualityGateCalculationRequestMock);

      doThrow(new QualityGateNotFoundException(QUALITY_GATE_CONFIG_NAME))
        .when(reportServiceMock)
        .initializeQualityGateCalculation(
          QUALITY_GATE_CONFIG_NAME,
          reportParameters
        );

      var response = fixture.calculateQualityGate(
        QUALITY_GATE_CONFIG_NAME,
        qualityGateCalculationRequestMock
      );

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(NOT_FOUND),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(CalculateQualityGate400Response.class))
              .satisfies(
                e -> assertThat(e.getCode()).isEqualTo("Not Found"),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    format(
                      "Quality-Gate configuration '%s' does not exist!",
                      QUALITY_GATE_CONFIG_NAME
                    )
                  )
              )
        );
    }
  }
}

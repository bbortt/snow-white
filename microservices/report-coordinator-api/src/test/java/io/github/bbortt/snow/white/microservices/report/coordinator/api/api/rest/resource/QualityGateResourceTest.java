/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ReportParameterMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate202Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGate400Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.CalculateQualityGateRequest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import java.util.Set;
import java.util.UUID;
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
  private ApiTestMapper apiTestMapperMock;

  @Mock
  private ReportParameterMapper reportParameterMapperMock;

  @Mock
  private QualityGateReportMapper qualityGateReportMapperMock;

  @Mock
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  private QualityGateResource fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateResource(
      reportServiceMock,
      apiTestMapperMock,
      reportParameterMapperMock,
      qualityGateReportMapperMock,
      reportCoordinationServicePropertiesMock
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
      var apiTests = Set.of(mock(ApiTest.class));
      doReturn(apiTests)
        .when(apiTestMapperMock)
        .getApiTests(qualityGateCalculationRequestMock);

      var reportParameter = mock(ReportParameter.class);
      doReturn(reportParameter)
        .when(reportParameterMapperMock)
        .fromDto(eq(qualityGateCalculationRequestMock), any(UUID.class));

      var qualityGateReport = mock(QualityGateReport.class);
      doReturn(UUID.fromString("37809fff-2044-4341-b55e-f99202291478"))
        .when(qualityGateReport)
        .getCalculationId();
      doReturn(qualityGateReport)
        .when(reportServiceMock)
        .initializeQualityGateCalculation(
          QUALITY_GATE_CONFIG_NAME,
          apiTests,
          reportParameter
        );

      var responseDto = mock(CalculateQualityGate202Response.class);
      doReturn(responseDto)
        .when(qualityGateReportMapperMock)
        .toDto(qualityGateReport);

      var apiGatewayHost = "http://my-api-gateway";
      doReturn(apiGatewayHost)
        .when(reportCoordinationServicePropertiesMock)
        .getPublicApiGatewayUrl();

      var response = fixture.calculateQualityGate(
        QUALITY_GATE_CONFIG_NAME,
        qualityGateCalculationRequestMock
      );

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(ACCEPTED),
          r -> assertThat(r.getBody()).isEqualTo(responseDto),
          r ->
            assertThat(r.getHeaders().toSingleValueMap()).containsEntry(
              LOCATION,
              "http://my-api-gateway/quality-gate/37809fff-2044-4341-b55e-f99202291478"
            )
        );
    }

    @Test
    void shouldReturnNotFoundResponse_whenConfigurationDoesNotExists()
      throws QualityGateNotFoundException {
      var reportParameter = mock(ReportParameter.class);
      doReturn(reportParameter)
        .when(reportParameterMapperMock)
        .fromDto(eq(qualityGateCalculationRequestMock), any(UUID.class));

      doThrow(new QualityGateNotFoundException(QUALITY_GATE_CONFIG_NAME))
        .when(reportServiceMock)
        .initializeQualityGateCalculation(
          QUALITY_GATE_CONFIG_NAME,
          emptySet(),
          reportParameter
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

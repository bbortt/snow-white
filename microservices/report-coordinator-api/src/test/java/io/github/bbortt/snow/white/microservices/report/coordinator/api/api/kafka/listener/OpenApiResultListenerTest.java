/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.TestResultForUnknownApiException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiResultListenerTest {

  @Mock
  private ApiTestResultMapper apiTestResultMapperMock;

  @Mock
  private QualityGateService qualityGateServiceMock;

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private ApiInformationFilter apiInformationFilterMock;

  @Mock
  private ApiTestResultLinker apiTestResultLinkerMock;

  @Mock
  private QualityGateStatusCalculator qualityGateStatusCalculatorMock;

  private OpenApiResultListener fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiResultListener(
      apiTestResultMapperMock,
      qualityGateServiceMock,
      reportServiceMock,
      apiInformationFilterMock,
      apiTestResultLinkerMock,
      qualityGateStatusCalculatorMock
    );
  }

  @Nested
  class Constructor {

    @Test
    void shouldInitializeWithDefaultHelperClasses() {
      var listener = new OpenApiResultListener(
        apiTestResultMapperMock,
        qualityGateServiceMock,
        reportServiceMock
      );

      assertThat(listener).isNotNull().hasNoNullFieldsOrProperties();
    }

    @Test
    void shouldInitializeWithCustomHelperClasses() {
      var listener = new OpenApiResultListener(
        apiTestResultMapperMock,
        qualityGateServiceMock,
        reportServiceMock,
        apiInformationFilterMock,
        apiTestResultLinkerMock,
        qualityGateStatusCalculatorMock
      );

      assertThat(listener).isNotNull().hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class PersistOpenApiCoverageResponseIfReportIsPresent {

    private static final UUID CALCULATION_ID = UUID.fromString(
      "9f679723-a328-47c6-b24e-e16894c675f1"
    );
    private static final String QUALITY_GATE_CONFIG_NAME = "test-config";

    @Test
    void shouldUpdateReport_whenReportAndQualityGateConfigExist()
      throws QualityGateNotFoundException {
      var originalReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(IN_PROGRESS)
        .build();

      var updatedReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(PASSED)
        .build();

      doReturn(Optional.of(originalReport))
        .when(reportServiceMock)
        .findReportByCalculationId(CALCULATION_ID);

      var qualityGateConfig = new QualityGateConfig(
        QUALITY_GATE_CONFIG_NAME,
        Set.of(PATH_COVERAGE.name())
      );
      doReturn(qualityGateConfig)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(QUALITY_GATE_CONFIG_NAME);

      var apiInformation = mock(ApiInformation.class);
      var apiTest = mock(ApiTest.class);

      doReturn(apiTest)
        .when(apiInformationFilterMock)
        .findApiTestMatchingApiInformationInQualityGateReport(
          originalReport,
          apiInformation
        );

      Set<OpenApiTestResult> openApiCriteria = Set.of(
        new OpenApiTestResult(
          PATH_COVERAGE,
          BigDecimal.valueOf(85.0),
          Duration.ofSeconds(1)
        )
      );

      var event = new OpenApiCoverageResponseEvent(
        OPENAPI,
        apiInformation,
        openApiCriteria
      );

      var apiTestResult = ApiTestResult.builder()
        .apiTestCriteria(PATH_COVERAGE.name())
        .coverage(BigDecimal.valueOf(85.0))
        .build();
      Set<ApiTestResult> mappedResults = Set.of(apiTestResult);

      doReturn(mappedResults)
        .when(apiTestResultMapperMock)
        .fromDtos(openApiCriteria);

      doReturn(updatedReport)
        .when(qualityGateStatusCalculatorMock)
        .withUpdatedReportStatus(originalReport);

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        CALCULATION_ID,
        event
      );

      verify(
        apiInformationFilterMock
      ).findApiTestMatchingApiInformationInQualityGateReport(
        originalReport,
        apiInformation
      );
      verify(qualityGateServiceMock).findQualityGateConfigByName(
        QUALITY_GATE_CONFIG_NAME
      );
      verify(apiTestResultMapperMock).fromDtos(openApiCriteria);
      verify(apiTestResultLinkerMock).addResultsToApiTest(
        apiTest,
        qualityGateConfig.getOpenApiCriteria(),
        mappedResults
      );
      verify(qualityGateStatusCalculatorMock).withUpdatedReportStatus(
        originalReport
      );
      verify(reportServiceMock).update(updatedReport);
    }

    @Test
    void shouldLogWarningAndReturn_whenReportDoesNotExist()
      throws QualityGateNotFoundException {
      var apiInformation = mock(ApiInformation.class);
      var event = new OpenApiCoverageResponseEvent(
        OPENAPI,
        apiInformation,
        emptySet()
      );

      doReturn(Optional.empty())
        .when(reportServiceMock)
        .findReportByCalculationId(CALCULATION_ID);

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        CALCULATION_ID,
        event
      );

      verifyNoInteractions(apiInformationFilterMock);
      verifyNoInteractions(qualityGateServiceMock);
      verifyNoInteractions(apiTestResultMapperMock);
      verifyNoInteractions(apiTestResultLinkerMock);
      verifyNoInteractions(qualityGateStatusCalculatorMock);
      verify(reportServiceMock, never()).update(any(QualityGateReport.class));
    }

    @Test
    void shouldPropagateQualityGateNotFoundException_whenQualityGateConfigDoesNotExist()
      throws QualityGateNotFoundException {
      var report = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(IN_PROGRESS)
        .build();

      var apiInformation = mock(ApiInformation.class);
      var apiTest = mock(ApiTest.class);
      var event = new OpenApiCoverageResponseEvent(
        OPENAPI,
        apiInformation,
        emptySet()
      );

      doReturn(Optional.of(report))
        .when(reportServiceMock)
        .findReportByCalculationId(CALCULATION_ID);

      doReturn(apiTest)
        .when(apiInformationFilterMock)
        .findApiTestMatchingApiInformationInQualityGateReport(
          report,
          apiInformation
        );

      doThrow(new QualityGateNotFoundException(QUALITY_GATE_CONFIG_NAME))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(QUALITY_GATE_CONFIG_NAME);

      assertThatThrownBy(() ->
        fixture.persistOpenApiCoverageResponseIfReportIsPresent(
          CALCULATION_ID,
          event
        )
      ).isInstanceOf(QualityGateNotFoundException.class);

      verify(
        apiInformationFilterMock
      ).findApiTestMatchingApiInformationInQualityGateReport(
        report,
        apiInformation
      );
      verify(qualityGateServiceMock).findQualityGateConfigByName(
        QUALITY_GATE_CONFIG_NAME
      );
      verifyNoInteractions(apiTestResultMapperMock);
      verifyNoInteractions(apiTestResultLinkerMock);
      verifyNoInteractions(qualityGateStatusCalculatorMock);
      verify(reportServiceMock, never()).update(any(QualityGateReport.class));
    }

    @Test
    void shouldPropagateTestResultForUnknownApiException_whenApiTestCannotBeFound() {
      var report = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(IN_PROGRESS)
        .build();

      var apiInformation = mock(ApiInformation.class);
      var event = new OpenApiCoverageResponseEvent(
        OPENAPI,
        apiInformation,
        emptySet()
      );

      doReturn(Optional.of(report))
        .when(reportServiceMock)
        .findReportByCalculationId(CALCULATION_ID);

      doThrow(new TestResultForUnknownApiException(report, apiInformation))
        .when(apiInformationFilterMock)
        .findApiTestMatchingApiInformationInQualityGateReport(
          report,
          apiInformation
        );

      assertThatThrownBy(() ->
        fixture.persistOpenApiCoverageResponseIfReportIsPresent(
          CALCULATION_ID,
          event
        )
      ).isInstanceOf(TestResultForUnknownApiException.class);

      verify(
        apiInformationFilterMock
      ).findApiTestMatchingApiInformationInQualityGateReport(
        report,
        apiInformation
      );
      verifyNoInteractions(qualityGateServiceMock);
      verifyNoInteractions(apiTestResultMapperMock);
      verifyNoInteractions(apiTestResultLinkerMock);
      verifyNoInteractions(qualityGateStatusCalculatorMock);
      verify(reportServiceMock, never()).update(any(QualityGateReport.class));
    }

    @Test
    void shouldHandleEmptyOpenApiCriteria()
      throws QualityGateNotFoundException {
      var originalReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(IN_PROGRESS)
        .build();

      var updatedReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(FAILED)
        .build();

      doReturn(Optional.of(originalReport))
        .when(reportServiceMock)
        .findReportByCalculationId(CALCULATION_ID);

      var qualityGateConfig = new QualityGateConfig(
        QUALITY_GATE_CONFIG_NAME,
        emptySet()
      );
      doReturn(qualityGateConfig)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(QUALITY_GATE_CONFIG_NAME);

      var apiInformation = mock(ApiInformation.class);
      var apiTest = mock(ApiTest.class);
      var event = new OpenApiCoverageResponseEvent(
        OPENAPI,
        apiInformation,
        emptySet()
      );

      doReturn(apiTest)
        .when(apiInformationFilterMock)
        .findApiTestMatchingApiInformationInQualityGateReport(
          originalReport,
          apiInformation
        );

      doReturn(emptySet()).when(apiTestResultMapperMock).fromDtos(emptySet());

      doReturn(updatedReport)
        .when(qualityGateStatusCalculatorMock)
        .withUpdatedReportStatus(originalReport);

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        CALCULATION_ID,
        event
      );

      verify(apiTestResultLinkerMock).addResultsToApiTest(
        apiTest,
        emptySet(),
        emptySet()
      );
      verify(reportServiceMock).update(updatedReport);
    }

    @Test
    void shouldHandleMultipleCriteriaResults()
      throws QualityGateNotFoundException {
      var originalReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(IN_PROGRESS)
        .build();

      var updatedReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(PASSED)
        .build();

      doReturn(Optional.of(originalReport))
        .when(reportServiceMock)
        .findReportByCalculationId(CALCULATION_ID);

      var qualityGateConfig = new QualityGateConfig(
        QUALITY_GATE_CONFIG_NAME,
        Set.of(PATH_COVERAGE.name(), "OPERATION_COVERAGE")
      );
      doReturn(qualityGateConfig)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(QUALITY_GATE_CONFIG_NAME);

      var apiInformation = mock(ApiInformation.class);
      var apiTest = mock(ApiTest.class);

      doReturn(apiTest)
        .when(apiInformationFilterMock)
        .findApiTestMatchingApiInformationInQualityGateReport(
          originalReport,
          apiInformation
        );

      Set<OpenApiTestResult> openApiCriteria = Set.of(
        new OpenApiTestResult(
          PATH_COVERAGE,
          BigDecimal.valueOf(90.0),
          Duration.ofSeconds(1)
        ),
        new OpenApiTestResult(
          HTTP_METHOD_COVERAGE,
          BigDecimal.valueOf(95.0),
          Duration.ofSeconds(2)
        )
      );

      var event = new OpenApiCoverageResponseEvent(
        OPENAPI,
        apiInformation,
        openApiCriteria
      );

      Set<ApiTestResult> mappedResults = Set.of(
        ApiTestResult.builder()
          .apiTestCriteria(PATH_COVERAGE.name())
          .coverage(BigDecimal.valueOf(90.0))
          .build(),
        ApiTestResult.builder()
          .apiTestCriteria(HTTP_METHOD_COVERAGE.name())
          .coverage(BigDecimal.valueOf(95.0))
          .build()
      );

      doReturn(mappedResults)
        .when(apiTestResultMapperMock)
        .fromDtos(openApiCriteria);

      doReturn(updatedReport)
        .when(qualityGateStatusCalculatorMock)
        .withUpdatedReportStatus(originalReport);

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        CALCULATION_ID,
        event
      );

      verify(apiTestResultLinkerMock).addResultsToApiTest(
        apiTest,
        qualityGateConfig.getOpenApiCriteria(),
        mappedResults
      );
      verify(reportServiceMock).update(updatedReport);
    }
  }
}

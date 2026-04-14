/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.TestData.minimalQualityGateReport;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FINISHED_EXCEPTIONALLY;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.ApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.QualityGateNotFoundException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith({ MockitoExtension.class })
class ReportServiceTest {

  @Mock
  private QualityGateService qualityGateServiceMock;

  @Mock
  private ApiTestRepository apiTestRepositoryMock;

  @Mock
  private QualityGateReportRepository qualityGateReportRepositoryMock;

  @Mock
  private ApiTestResultMapper apiTestResultMapperMock;

  @Mock
  private QualityGateReportApiTestsFilter qualityGateReportApiTestsFilterMock;

  @Mock
  private ApiTestResultLinker apiTestResultLinkerMock;

  @Mock
  private QualityGateStatusCalculator qualityGateStatusCalculatorMock;

  @Mock
  private QualityGateCalculationRequestDispatcher dispatcherMock;

  private ReportService fixture;

  @BeforeEach
  void beforeEach() {
    fixture = new ReportService(
      qualityGateServiceMock,
      apiTestRepositoryMock,
      qualityGateReportRepositoryMock,
      apiTestResultMapperMock,
      qualityGateReportApiTestsFilterMock,
      apiTestResultLinkerMock,
      qualityGateStatusCalculatorMock,
      dispatcherMock
    );
  }

  @Nested
  class FindReportByCalculationIdTest {

    @Test
    void shouldReturnQualityGateReportById() {
      var calculationId = UUID.fromString(
        "85b5b089-9fe0-46c2-9fa4-e7eaaf33e0d3"
      );
      var qualityGateReport = new QualityGateReport();

      doReturn(Optional.of(qualityGateReport))
        .when(qualityGateReportRepositoryMock)
        .findById(calculationId);

      var result = fixture.findReportByCalculationId(calculationId);

      assertThat(result).isPresent().get().isEqualTo(qualityGateReport);
    }
  }

  @Nested
  class UpdateReportWithOpenApiCoverageResultsTest {

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
        .reportStatus(IN_PROGRESS.getVal())
        .reportParameter(mock(ReportParameter.class))
        .build();

      var updatedReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportStatus(PASSED.getVal())
        .reportParameter(mock(ReportParameter.class))
        .build();

      doReturn(Optional.of(originalReport))
        .when(qualityGateReportRepositoryMock)
        .findById(CALCULATION_ID);

      var qualityGateConfig = new QualityGateConfig(
        QUALITY_GATE_CONFIG_NAME,
        Set.of("PATH_COVERAGE")
      );
      doReturn(qualityGateConfig)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(QUALITY_GATE_CONFIG_NAME);

      var apiInformation = mock(ApiInformation.class);
      var apiTest = mock(ApiTest.class);

      doReturn(apiTest)
        .when(qualityGateReportApiTestsFilterMock)
        .findApiTestMatchingApiInformationInQualityGateReport(
          originalReport,
          apiInformation
        );

      var event = new OpenApiCoverageResponseEvent(apiInformation, emptySet());

      Set<ApiTestResult> mappedResults = Set.of(mock(ApiTestResult.class));
      doReturn(mappedResults)
        .when(apiTestResultMapperMock)
        .fromDtos(emptySet(), apiTest);
      doReturn(updatedReport)
        .when(qualityGateStatusCalculatorMock)
        .withUpdatedReportStatus(originalReport);

      fixture.updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      verify(
        qualityGateReportApiTestsFilterMock
      ).findApiTestMatchingApiInformationInQualityGateReport(
        originalReport,
        apiInformation
      );
      verify(qualityGateServiceMock).findQualityGateConfigByName(
        QUALITY_GATE_CONFIG_NAME
      );
      verify(apiTestResultMapperMock).fromDtos(emptySet(), apiTest);
      verify(apiTestResultLinkerMock).addApiTestResultsToApiTest(
        mappedResults,
        apiTest,
        qualityGateConfig.getOpenApiCriteria()
      );
      verify(qualityGateStatusCalculatorMock).withUpdatedReportStatus(
        originalReport
      );
      verify(qualityGateReportRepositoryMock).save(updatedReport);
    }

    @Test
    void shouldLogWarningAndReturn_whenReportDoesNotExist() {
      var event = new OpenApiCoverageResponseEvent(
        mock(ApiInformation.class),
        emptySet()
      );

      doReturn(Optional.empty())
        .when(qualityGateReportRepositoryMock)
        .findById(CALCULATION_ID);

      fixture.updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      verifyNoInteractions(qualityGateReportApiTestsFilterMock);
      verifyNoInteractions(qualityGateServiceMock);
      verifyNoInteractions(apiTestResultMapperMock);
      verifyNoInteractions(apiTestResultLinkerMock);
      verifyNoInteractions(qualityGateStatusCalculatorMock);
      verify(qualityGateReportRepositoryMock, never()).save(any());
    }

    @Test
    void shouldMarkReportAsFinishedExceptionally_whenEventCarriesException() {
      var originalReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportParameter(mock())
        .reportStatus(IN_PROGRESS.getVal())
        .build();

      doReturn(Optional.of(originalReport))
        .when(qualityGateReportRepositoryMock)
        .findById(CALCULATION_ID);

      doAnswer(returnsFirstArg())
        .when(qualityGateStatusCalculatorMock)
        .withUpdatedReportStatus(any(QualityGateReport.class));

      var event = new OpenApiCoverageResponseEvent(
        mock(ApiInformation.class),
        "upstream failure"
      );

      fixture.updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      verifyNoInteractions(qualityGateServiceMock);
      verifyNoInteractions(qualityGateReportApiTestsFilterMock);
      verifyNoInteractions(apiTestResultMapperMock);
      verifyNoInteractions(apiTestResultLinkerMock);

      ArgumentCaptor<QualityGateReport> reportCaptor = captor();
      verify(qualityGateStatusCalculatorMock).withUpdatedReportStatus(
        reportCaptor.capture()
      );

      var qualityGateReportWithStackTrace = reportCaptor.getValue();
      assertThat(qualityGateReportWithStackTrace.getReportStatus()).isEqualTo(
        FINISHED_EXCEPTIONALLY
      );
      assertThat(qualityGateReportWithStackTrace.getStackTrace()).contains(
        "upstream failure"
      );

      verify(qualityGateReportRepositoryMock).save(
        qualityGateReportWithStackTrace
      );
    }

    @Test
    void shouldLogWarningAndReturn_whenQualityGateConfigNotFound()
      throws QualityGateNotFoundException {
      var originalReport = QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName(QUALITY_GATE_CONFIG_NAME)
        .reportParameter(mock())
        .build();

      doReturn(Optional.of(originalReport))
        .when(qualityGateReportRepositoryMock)
        .findById(CALCULATION_ID);

      doThrow(new QualityGateNotFoundException(QUALITY_GATE_CONFIG_NAME))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(QUALITY_GATE_CONFIG_NAME);

      var event = new OpenApiCoverageResponseEvent(
        mock(ApiInformation.class),
        emptySet()
      );

      fixture.updateReportWithOpenApiCoverageResults(CALCULATION_ID, event);

      verifyNoInteractions(apiTestResultMapperMock);
      verifyNoInteractions(apiTestResultLinkerMock);
      verifyNoInteractions(qualityGateStatusCalculatorMock);
      verify(qualityGateReportRepositoryMock, never()).save(any());
    }
  }

  @Nested
  class InitializeQualityGateCalculationTest {

    @Test
    void shouldPersistReportAndApiTests_andDelegateDispatch()
      throws QualityGateNotFoundException {
      var qualityGateConfigName = "test-config";
      var apiTest = ApiTest.builder()
        .serviceName("test-service")
        .apiName("test-api")
        .apiVersion("test-api-version")
        .apiType(OPENAPI.getVal())
        .build();
      var apiTests = Set.of(apiTest);

      var reportParameter = ReportParameter.builder()
        .calculationId(UUID.fromString("41386eb3-7569-4944-a39f-0bdcadf15654"))
        .lookbackWindow("1d")
        .build();

      doReturn(new QualityGateConfig(qualityGateConfigName, emptySet()))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      var savedReport = minimalQualityGateReport(
        UUID.fromString("6f465636-2ea3-4279-80db-6ff1643df6af")
      );
      doReturn(savedReport)
        .when(qualityGateReportRepositoryMock)
        .save(any(QualityGateReport.class));

      doAnswer(returnsFirstArg())
        .when(apiTestRepositoryMock)
        .save(any(ApiTest.class));

      var result = fixture.initializeQualityGateCalculation(
        qualityGateConfigName,
        apiTests,
        reportParameter
      );

      assertThat(result).isNotNull();
      assertThat(result.getApiTests())
        .hasSize(1)
        .allSatisfy(persistedApiTest ->
          assertThat(persistedApiTest.getQualityGateReport()).isEqualTo(
            savedReport
          )
        );

      ArgumentCaptor<QualityGateReport> reportCaptor = captor();
      verify(qualityGateReportRepositoryMock).save(reportCaptor.capture());
      assertThat(reportCaptor.getValue()).satisfies(
        r ->
          assertThat(r.getQualityGateConfigName()).isEqualTo(
            qualityGateConfigName
          ),
        r ->
          assertThat(r.getReportParameter().getLookbackWindow()).isEqualTo(
            reportParameter.getLookbackWindow()
          )
      );

      verify(dispatcherMock).dispatch(
        savedReport.getCalculationId(),
        savedReport.getReportParameter(),
        result.getApiTests()
      );
    }

    @Test
    void shouldThrowQualityGateNotFoundException_whenQualityGateConfigNotFound()
      throws QualityGateNotFoundException {
      var qualityGateConfigName = "non-existent-config";
      var reportParameter = new ReportParameter();

      var cause = new QualityGateNotFoundException(qualityGateConfigName);
      doThrow(cause)
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      assertThatThrownBy(() ->
        fixture.initializeQualityGateCalculation(
          qualityGateConfigName,
          emptySet(),
          reportParameter
        )
      ).isEqualTo(cause);
    }
  }

  @Nested
  class UpdateTest {

    @Test
    void shouldDelegateToRepositoryAndReturnSavedReport() {
      var report = new QualityGateReport();
      var savedReport = new QualityGateReport();

      doReturn(savedReport).when(qualityGateReportRepositoryMock).save(report);

      assertThat(fixture.update(report)).isEqualTo(savedReport);
    }
  }

  @Nested
  class FindAllReportsTest {

    @Test
    void shouldDelegateToRepository() {
      var pageable = Pageable.unpaged();
      var page = Page.empty();

      doReturn(page).when(qualityGateReportRepositoryMock).findAll(pageable);

      Page<@NonNull QualityGateReport> result = fixture.findAllReports(
        pageable
      );

      assertThat(result).isEqualTo(page);
    }
  }
}

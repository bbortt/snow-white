/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka.listener;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestCriteria;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.OpenApiTestResultMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.QualityGateService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiResultListenerTest {

  @Mock
  private OpenApiTestResultMapper openApiTestResultMapperMock;

  @Mock
  private QualityGateService qualityGateServiceMock;

  @Mock
  private ReportService reportServiceMock;

  private OpenApiResultListener fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiResultListener(
      openApiTestResultMapperMock,
      qualityGateServiceMock,
      reportServiceMock
    );
  }

  @Nested
  class PersistOpenApiCoverageResponseIfReportIsPresent {

    private final UUID calculationId = UUID.fromString(
      "9f679723-a328-47c6-b24e-e16894c675f1"
    );
    private final String qualityGateConfigName = "test-config";

    @Test
    void shouldUpdateReportWhenReportAndQualityGateConfigExist() {
      QualityGateReport report = QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName(qualityGateConfigName)
        .reportStatus(IN_PROGRESS)
        .build();
      doReturn(Optional.of(report))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      var qualityGateConfig = new QualityGateConfig(
        qualityGateConfigName,
        singletonList(PATH_COVERAGE.name())
      );
      doReturn(Optional.of(qualityGateConfig))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      Set<
        io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult
      > openApiCriteria = Set.of(
        new io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult(
          PATH_COVERAGE,
          BigDecimal.valueOf(85.0),
          Duration.ofSeconds(1)
        )
      );
      var event = new OpenApiCoverageResponseEvent(openApiCriteria);

      OpenApiTestResult openApiTestResult = OpenApiTestResult.builder()
        .openApiTestCriteria(
          OpenApiTestCriteria.builder().name("test-criterion").build()
        )
        .coverage(BigDecimal.valueOf(85.0))
        .build();
      Set<OpenApiTestResult> mappedResults = Set.of(openApiTestResult);

      doReturn(mappedResults)
        .when(openApiTestResultMapperMock)
        .fromDto(openApiCriteria);

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        calculationId,
        event
      );

      ArgumentCaptor<QualityGateReport> qualityGateReportArgumentCaptor =
        captor();
      verify(reportServiceMock).update(
        qualityGateReportArgumentCaptor.capture()
      );
      assertThat(qualityGateReportArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getCalculationId()).isEqualTo(calculationId),
          r ->
            assertThat(r.getQualityGateConfigName()).isEqualTo(
              qualityGateConfigName
            ),
          r -> assertThat(r.getOpenApiCoverageStatus()).isEqualTo(FAILED),
          r -> assertThat(r.getOpenApiTestResults()).hasSize(1),
          r -> assertThat(r.getReportStatus()).isEqualTo(FAILED),
          r -> assertThat(r.getCreatedAt()).isNotNull()
        );
    }

    @Test
    void shouldNotProcessWhenReportDoesNotExist() {
      var event = new OpenApiCoverageResponseEvent(emptySet());
      doReturn(Optional.empty())
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        calculationId,
        event
      );

      verifyNoInteractions(qualityGateServiceMock);
      verifyNoInteractions(openApiTestResultMapperMock);
      verify(reportServiceMock, never()).update(any(QualityGateReport.class));
    }

    @Test
    void shouldThrow_whenQualityGateConfigDoesNotExist() {
      QualityGateReport report = QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName(qualityGateConfigName)
        .reportStatus(IN_PROGRESS)
        .build();

      var event = new OpenApiCoverageResponseEvent(emptySet());

      doReturn(Optional.of(report))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);
      doReturn(Optional.empty())
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      assertThatThrownBy(() ->
        fixture.persistOpenApiCoverageResponseIfReportIsPresent(
          calculationId,
          event
        )
      )
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
          "Unreachable state, Quality-Gate configuration '%s' must exist at this point!",
          qualityGateConfigName
        );

      verifyNoInteractions(openApiTestResultMapperMock);
      verify(reportServiceMock, never()).update(any(QualityGateReport.class));
    }

    @Test
    void shouldCalculatePassedStatusWhenAllCriteriaPass() {
      QualityGateReport report = QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName(qualityGateConfigName)
        .reportStatus(IN_PROGRESS)
        .build();

      var qualityGateConfig = new QualityGateConfig(
        qualityGateConfigName,
        List.of(PATH_COVERAGE.name(), HTTP_METHOD_COVERAGE.name())
      );
      doReturn(Optional.of(report))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);
      doReturn(Optional.of(qualityGateConfig))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      Set<
        io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult
      > openApiCriteria = Set.of(
        new io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult(
          PATH_COVERAGE,
          ONE,
          Duration.ofSeconds(1)
        ),
        new io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult(
          HTTP_METHOD_COVERAGE,
          ONE,
          Duration.ofSeconds(1)
        )
      );
      var event = new OpenApiCoverageResponseEvent(openApiCriteria);

      OpenApiTestResult criterion1Result = OpenApiTestResult.builder()
        .openApiTestCriteria(
          OpenApiTestCriteria.builder().name(PATH_COVERAGE.name()).build()
        )
        .coverage(ONE)
        .build();

      OpenApiTestResult criterion2Result = OpenApiTestResult.builder()
        .openApiTestCriteria(
          OpenApiTestCriteria.builder()
            .name(HTTP_METHOD_COVERAGE.name())
            .build()
        )
        .coverage(ONE)
        .build();

      Set<OpenApiTestResult> mappedResults = Set.of(
        criterion1Result,
        criterion2Result
      );

      doReturn(mappedResults)
        .when(openApiTestResultMapperMock)
        .fromDto(openApiCriteria);

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        calculationId,
        event
      );

      ArgumentCaptor<QualityGateReport> qualityGateReportArgumentCaptor =
        captor();
      verify(reportServiceMock).update(
        qualityGateReportArgumentCaptor.capture()
      );
      assertThat(qualityGateReportArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getCalculationId()).isEqualTo(calculationId),
          r ->
            assertThat(r.getQualityGateConfigName()).isEqualTo(
              qualityGateConfigName
            ),
          r -> assertThat(r.getOpenApiCoverageStatus()).isEqualTo(PASSED),
          r -> assertThat(r.getOpenApiTestResults()).hasSize(2),
          r -> assertThat(r.getReportStatus()).isEqualTo(PASSED)
        );
    }

    @Test
    void shouldCalculateFailedStatusWhenAnyCriterionFails() {
      QualityGateReport report = QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName(qualityGateConfigName)
        .reportStatus(IN_PROGRESS)
        .build();

      var qualityGateConfig = new QualityGateConfig(
        qualityGateConfigName,
        asList(PATH_COVERAGE.name(), HTTP_METHOD_COVERAGE.name())
      );
      doReturn(Optional.of(report))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);
      doReturn(Optional.of(qualityGateConfig))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      Set<
        io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult
      > openApiCriteria = Set.of(
        new io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult(
          PATH_COVERAGE,
          ONE,
          Duration.ofSeconds(1)
        ),
        new io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult(
          HTTP_METHOD_COVERAGE,
          BigDecimal.valueOf(85.0),
          Duration.ofSeconds(1)
        )
      );
      var event = new OpenApiCoverageResponseEvent(openApiCriteria);

      OpenApiTestResult criterion1Result = OpenApiTestResult.builder()
        .openApiTestCriteria(
          OpenApiTestCriteria.builder().name(PATH_COVERAGE.name()).build()
        )
        .coverage(ONE)
        .build();

      OpenApiTestResult criterion2Result = OpenApiTestResult.builder()
        .openApiTestCriteria(
          OpenApiTestCriteria.builder()
            .name(HTTP_METHOD_COVERAGE.name())
            .build()
        )
        .coverage(BigDecimal.valueOf(85.0))
        .build();

      Set<OpenApiTestResult> mappedResults = new HashSet<>(
        asList(criterion1Result, criterion2Result)
      );

      doReturn(mappedResults)
        .when(openApiTestResultMapperMock)
        .fromDto(openApiCriteria);

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        calculationId,
        event
      );

      ArgumentCaptor<QualityGateReport> qualityGateReportArgumentCaptor =
        captor();
      verify(reportServiceMock).update(
        qualityGateReportArgumentCaptor.capture()
      );
      assertThat(qualityGateReportArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getCalculationId()).isEqualTo(calculationId),
          r ->
            assertThat(r.getQualityGateConfigName()).isEqualTo(
              qualityGateConfigName
            ),
          r -> assertThat(r.getOpenApiCoverageStatus()).isEqualTo(FAILED),
          r -> assertThat(r.getOpenApiTestResults()).hasSize(2),
          r -> assertThat(r.getReportStatus()).isEqualTo(FAILED)
        );
    }

    @Test
    void shouldUpdateWithEmptyResultsWhenNoCriteriaProvided() {
      QualityGateReport report = QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName(qualityGateConfigName)
        .reportStatus(IN_PROGRESS)
        .build();

      var qualityGateConfig = new QualityGateConfig(
        qualityGateConfigName,
        emptyList()
      );
      doReturn(Optional.of(report))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);
      doReturn(Optional.of(qualityGateConfig))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      var event = new OpenApiCoverageResponseEvent(emptySet());

      doReturn(emptySet())
        .when(openApiTestResultMapperMock)
        .fromDto(emptySet());

      fixture.persistOpenApiCoverageResponseIfReportIsPresent(
        calculationId,
        event
      );

      ArgumentCaptor<QualityGateReport> qualityGateReportArgumentCaptor =
        captor();
      verify(reportServiceMock).update(
        qualityGateReportArgumentCaptor.capture()
      );
      assertThat(qualityGateReportArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getCalculationId()).isEqualTo(calculationId),
          r ->
            assertThat(r.getQualityGateConfigName()).isEqualTo(
              qualityGateConfigName
            ),
          r -> assertThat(r.getOpenApiCoverageStatus()).isEqualTo(PASSED),
          r -> assertThat(r.getOpenApiTestResults()).isEmpty(),
          r -> assertThat(r.getReportStatus()).isEqualTo(PASSED)
        );
    }
  }
}

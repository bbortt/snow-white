/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.service;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.exception.QualityGateNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith({ MockitoExtension.class })
class ReportServiceTest {

  @Mock
  private KafkaTemplate<
    String,
    QualityGateCalculationRequestEvent
  > kafkaTemplateMock;

  @Mock
  private QualityGateReportRepository qualityGateReportRepositoryMock;

  @Mock
  private QualityGateService qualityGateServiceMock;

  private ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  private ReportService fixture;

  @BeforeEach
  void beforeEachSetup() {
    reportCoordinationServiceProperties =
      new ReportCoordinationServiceProperties();

    fixture = new ReportService(
      kafkaTemplateMock,
      qualityGateReportRepositoryMock,
      reportCoordinationServiceProperties,
      qualityGateServiceMock
    );
  }

  @Nested
  class FindReportByCalculationId {

    @Test
    void shouldReturnQualityGateReportById() {
      var reportCalculationId = UUID.fromString(
        "85b5b089-9fe0-46c2-9fa4-e7eaaf33e0d3"
      );

      var qualityGateReport = new QualityGateReport();
      doReturn(Optional.of(qualityGateReport))
        .when(qualityGateReportRepositoryMock)
        .findById(reportCalculationId);

      var result = fixture.findReportByCalculationId(reportCalculationId);

      assertThat(result).isPresent().get().isEqualTo(qualityGateReport);
    }
  }

  @Nested
  class InitializeQualityGateCalculation {

    @Test
    void shouldCreateAndReturnQualityGateReport()
      throws QualityGateNotFoundException {
      var qualityGateConfigName = "test-config";
      var reportParameters = ReportParameters.builder()
        .serviceName("test-service")
        .apiName("test-api")
        .apiVersion("v1")
        .lookbackWindow("1d")
        .build();

      var qualityGateConfig = new QualityGateConfig(
        qualityGateConfigName,
        emptyList()
      );

      doReturn(Optional.of(qualityGateConfig))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      var initialQualityGateReport = QualityGateReport.builder()
        .calculationId(UUID.fromString("a1c937c2-d950-4047-8c8d-f1de16c13b41"))
        .qualityGateConfigName(qualityGateConfigName)
        .reportParameters(reportParameters)
        .build();

      doReturn(initialQualityGateReport)
        .when(qualityGateReportRepositoryMock)
        .save(any(QualityGateReport.class));

      var result = fixture.initializeQualityGateCalculation(
        qualityGateConfigName,
        reportParameters
      );

      assertThat(result).isEqualTo(initialQualityGateReport);
      verify(kafkaTemplateMock).send(
        eq(reportCoordinationServiceProperties.getCalculationRequestTopic()),
        eq(initialQualityGateReport.getCalculationId().toString()),
        any(QualityGateCalculationRequestEvent.class)
      );
    }

    @Test
    void shouldThrowQualityGateNotFoundExceptionWhenQualityGateConfigNotFound() {
      var qualityGateConfigName = "non-existent-config";
      var reportParameters = new ReportParameters();

      doReturn(Optional.empty())
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      assertThatThrownBy(() ->
        fixture.initializeQualityGateCalculation(
          qualityGateConfigName,
          reportParameters
        )
      )
        .isInstanceOf(QualityGateNotFoundException.class)
        .hasMessage(
          format(
            "No Quality-Gate configuration with ID '%s' exists!",
            qualityGateConfigName
          )
        );
    }

    @Test
    void shouldCreateQualityGateReportWithCorrectValues()
      throws QualityGateNotFoundException {
      var qualityGateConfigName = "test-config";
      var reportParameters = ReportParameters.builder()
        .serviceName("test-service")
        .apiName("test-api")
        .apiVersion("v1")
        .lookbackWindow("1d")
        .build();

      var qualityGateConfig = new QualityGateConfig(
        qualityGateConfigName,
        emptyList()
      );

      doReturn(Optional.of(qualityGateConfig))
        .when(qualityGateServiceMock)
        .findQualityGateConfigByName(qualityGateConfigName);

      // Capture the QualityGateReport being saved
      ArgumentCaptor<QualityGateReport> reportCaptor = captor();

      var savedQualityGateReport = QualityGateReport.builder()
        .calculationId(UUID.fromString("6f465636-2ea3-4279-80db-6ff1643df6af"))
        .reportParameters(reportParameters)
        .build();
      doReturn(savedQualityGateReport)
        .when(qualityGateReportRepositoryMock)
        .save(reportCaptor.capture());

      fixture.initializeQualityGateCalculation(
        qualityGateConfigName,
        reportParameters
      );

      QualityGateReport capturedReport = reportCaptor.getValue();
      assertThat(capturedReport.getQualityGateConfigName()).isEqualTo(
        qualityGateConfigName
      );
      assertThat(capturedReport.getReportParameters()).isEqualTo(
        reportParameters
      );
    }
  }

  @Nested
  class Update {

    @Test
    void shouldReturnUpdatedQualityGateReport() {
      var qualityGateReport = new QualityGateReport();

      var updatedQualityGateReport = new QualityGateReport();
      doReturn(updatedQualityGateReport)
        .when(qualityGateReportRepositoryMock)
        .save(qualityGateReport);

      var result = fixture.update(qualityGateReport);

      assertThat(result).isEqualTo(updatedQualityGateReport);
    }
  }
}

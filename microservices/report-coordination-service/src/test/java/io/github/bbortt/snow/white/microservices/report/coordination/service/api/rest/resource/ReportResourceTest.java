/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.JUnitReportCreationException;
import io.github.bbortt.snow.white.microservices.report.coordination.service.junit.JUnitReporter;
import io.github.bbortt.snow.white.microservices.report.coordination.service.service.ReportService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class ReportResourceTest {

  @Mock
  private JUnitReporter jUnitReporterMock;

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private QualityGateReportMapper qualityGateReportMapperMock;

  private ReportResource fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ReportResource(
      jUnitReporterMock,
      reportServiceMock,
      qualityGateReportMapperMock
    );
  }

  @Nested
  class GetReportByCalculationId {

    @Mock
    private QualityGateReport qualityGateReport;

    io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport configureServiceMock(
      UUID calculationId
    ) {
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      var responseDto = mock(
        io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.class
      );
      doReturn(responseDto)
        .when(qualityGateReportMapperMock)
        .toDto(qualityGateReport);

      return responseDto;
    }

    @Test
    void shouldReturnReport_inSuccessStatus() {
      var calculationId = UUID.fromString(
        "35e9b4bf-6d9b-46d8-993c-feff1371c1fa"
      );
      var responseDto = configureServiceMock(calculationId);

      assertThatResponseIsStatusOkWithDto(PASSED, calculationId, responseDto);
    }

    @Test
    void shouldReturnReport_inStatusProgress() {
      var calculationId = UUID.fromString(
        "6edca9e1-6a3a-426a-a32a-7e970b52886e"
      );
      var responseDto = configureServiceMock(calculationId);

      doReturn(IN_PROGRESS).when(qualityGateReport).getReportStatus();

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseHasBody(response, ACCEPTED, responseDto);
    }

    @Test
    void shouldReturnReport_inFailureStatus() {
      var calculationId = UUID.fromString(
        "8c0fb130-1005-4b9a-a8dc-bce77a8f121e"
      );
      var responseDto = configureServiceMock(calculationId);

      assertThatResponseIsStatusOkWithDto(FAILED, calculationId, responseDto);
    }

    @Test
    void shouldReturnHttpNotFound_whenReportByCalculationIdNotFound() {
      var calculationId = UUID.fromString(
        "68fa43e1-df3a-4f52-a5d4-e8d88696c85e"
      );
      doReturn(Optional.empty())
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      var response = fixture.getReportByCalculationId(calculationId);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(NOT_FOUND),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(Error.class))
              .satisfies(
                e ->
                  assertThat(e.getCode()).isEqualTo(
                    NOT_FOUND.getReasonPhrase()
                  ),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    format("No report by id '%s' exists!", calculationId)
                  )
              )
        );
    }

    private void assertThatResponseIsStatusOkWithDto(
      ReportStatus failed,
      UUID calculationId,
      io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport responseDto
    ) {
      doReturn(failed).when(qualityGateReport).getReportStatus();

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseHasBody(response, OK, responseDto);
    }

    private static void assertThatResponseHasBody(
      ResponseEntity response,
      HttpStatus ok,
      io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport responseDto
    ) {
      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(ok),
          r -> assertThat(r.getBody()).isEqualTo(responseDto)
        );
    }
  }

  @Nested
  class GetReportByCalculationIdAsJUnit {

    @Mock
    private QualityGateReport qualityGateReport;

    @Test
    void shouldReturnJUnitReport() throws JUnitReportCreationException {
      var calculationId = UUID.fromString(
        "81699bec-99a0-4c8f-a9d0-06729477fe00"
      );
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      var junitReport = mock(Resource.class);
      doReturn(junitReport)
        .when(jUnitReporterMock)
        .transformToJUnitReport(qualityGateReport);

      var response = fixture.getReportByCalculationIdAsJUnit(calculationId);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isEqualTo(junitReport)
        );
    }

    @Test
    void shouldReturnHttpNotFound_whenReportByCalculationIdNotFound() {
      var calculationId = UUID.fromString(
        "12cfbdd4-f2f2-4b16-98fa-5dde81be1541"
      );
      doReturn(Optional.empty())
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      var response = fixture.getReportByCalculationIdAsJUnit(calculationId);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(NOT_FOUND),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(Error.class))
              .satisfies(
                e ->
                  assertThat(e.getCode()).isEqualTo(
                    NOT_FOUND.getReasonPhrase()
                  ),
                e ->
                  assertThat(e.getMessage()).isEqualTo(
                    format("No report by id '%s' exists!", calculationId)
                  )
              )
        );
    }

    @Test
    void shouldReturnInternalServerError_whenJUnitReportCreationFails()
      throws JUnitReportCreationException {
      var calculationId = UUID.fromString(
        "1797354a-4230-4dd2-b1d2-2923f6424d05"
      );
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      var cause = mock(JUnitReportCreationException.class);
      doThrow(cause)
        .when(jUnitReporterMock)
        .transformToJUnitReport(qualityGateReport);

      var message = "some error message";
      doReturn(message).when(cause).getMessage();

      var response = fixture.getReportByCalculationIdAsJUnit(calculationId);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(INTERNAL_SERVER_ERROR),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(Error.class))
              .satisfies(
                e ->
                  assertThat(e.getCode()).isEqualTo(
                    INTERNAL_SERVER_ERROR.getReasonPhrase()
                  ),
                e -> assertThat(e.getMessage()).isEqualTo(message)
              )
        );
    }
  }

  @Nested
  class ListQualityGateReports {

    @Test
    void shouldReturnListOfQualityGateReports() {
      var page = 0;
      var size = 10;
      var sort = "createdAt,desc";

      var report1 = mock(QualityGateReport.class);
      var report2 = mock(QualityGateReport.class);

      Page<QualityGateReport> qualityGateReportsPage = mock();
      doReturn(qualityGateReportsPage)
        .when(reportServiceMock)
        .findAllFinishedReports(any(Pageable.class));

      List<QualityGateReport> reportList = List.of(report1, report2);
      doReturn(reportList.stream()).when(qualityGateReportsPage).get();

      io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport dto1 =
        mock(
          io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.class
        );
      io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport dto2 =
        mock(
          io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.class
        );

      doReturn(dto1).when(qualityGateReportMapperMock).toDto(report1);
      doReturn(dto2).when(qualityGateReportMapperMock).toDto(report2);

      ResponseEntity<
        List<
          io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport
        >
      > response = fixture.listQualityGateReports(page, size, sort);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).containsExactly(dto1, dto2),
          r ->
            assertThat(r.getHeaders())
              .hasSize(1)
              .hasEntrySatisfying(HEADER_X_TOTAL_COUNT, value ->
                assertThat(value).containsExactly("0")
              )
        );
    }

    @Test
    void shouldHandleEmptyListOfQualityGateReports() {
      var page = 0;
      var size = 10;
      var sort = "createdAt,desc";

      Page<QualityGateReport> qualityGateReportsPage = mock();
      doReturn(qualityGateReportsPage)
        .when(reportServiceMock)
        .findAllFinishedReports(any(Pageable.class));

      doReturn(Stream.empty()).when(qualityGateReportsPage).get();

      ResponseEntity<
        List<
          io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport
        >
      > response = fixture.listQualityGateReports(page, size, sort);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isEmpty(),
          r ->
            assertThat(r.getHeaders())
              .hasSize(1)
              .hasEntrySatisfying(HEADER_X_TOTAL_COUNT, value ->
                assertThat(value).containsExactly("0")
              )
        );

      verifyNoInteractions(qualityGateReportMapperMock);
    }
  }
}

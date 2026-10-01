/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper.QualityGateReportMapper;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.GetReportByCalculationId200Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports500Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.JUnitReporter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.junit.TestSuites;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.ReportService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith({ MockitoExtension.class })
class ReportResourceUnitTest {

  @Mock
  private JUnitReporter jUnitReporterMock;

  @Mock
  private ReportService reportServiceMock;

  @Mock
  private QualityGateReportMapper qualityGateReportMapperMock;

  @InjectMocks
  private ReportResource fixture;

  @Nested
  class GetReportByCalculationIdTest {

    @Mock
    private QualityGateReport qualityGateReport;

    /**
     * The status is projected first and decides which report gets read, so a completed path stubs
     * the status and the findings-carrying read - see
     * {@code ReportResource#getReportByCalculationId(UUID)}.
     */
    GetReportByCalculationId200Response configureServiceMock(
      UUID calculationId,
      ReportStatus reportStatus
    ) {
      doReturn(Optional.of(reportStatus))
        .when(reportServiceMock)
        .findReportStatusByCalculationId(calculationId);
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportWithFindingsByCalculationId(calculationId);

      var responseDto = mock(GetReportByCalculationId200Response.class);
      doReturn(responseDto)
        .when(qualityGateReportMapperMock)
        .toReportDto(qualityGateReport);

      return responseDto;
    }

    @Test
    void shouldReturnReport_inSuccessStatus() {
      var calculationId = UUID.fromString(
        "35e9b4bf-6d9b-46d8-993c-feff1371c1fa"
      );

      assertThatResponseIsStatusOkWithDto(PASSED, calculationId);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
    @VerifiesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
    void shouldReturnReport_inStatusProgress() {
      var calculationId = UUID.fromString(
        "6edca9e1-6a3a-426a-a32a-7e970b52886e"
      );
      doReturn(Optional.of(IN_PROGRESS))
        .when(reportServiceMock)
        .findReportStatusByCalculationId(calculationId);
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);
      doReturn(IN_PROGRESS).when(qualityGateReport).getReportStatus();

      var responseDto = mock(ListQualityGateReports200ResponseInner.class);
      doReturn(responseDto)
        .when(qualityGateReportMapperMock)
        .toListDto(qualityGateReport);

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseHasBody(response, ACCEPTED, responseDto);

      // This endpoint is also the calculation's poll, and a poll reads nothing but 'status': the
      // findings-free shape is what keeps it off the evidence table.
      verify(reportServiceMock, never()).findReportWithFindingsByCalculationId(
        any()
      );
    }

    @Test
    void shouldReturnReport_inFailureStatus() {
      var calculationId = UUID.fromString(
        "8c0fb130-1005-4b9a-a8dc-bce77a8f121e"
      );

      assertThatResponseIsStatusOkWithDto(FAILED, calculationId);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
    void shouldReturnHttpNotFound_whenReportByCalculationIdNotFound() {
      var calculationId = UUID.fromString(
        "68fa43e1-df3a-4f52-a5d4-e8d88696c85e"
      );
      doReturn(Optional.empty())
        .when(reportServiceMock)
        .findReportStatusByCalculationId(calculationId);

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseIsNotFound(response, calculationId);
      verifyNoInteractions(qualityGateReportMapperMock);
    }

    /**
     * The status and the report are not one atomic look: a report deleted between them would
     * otherwise map a missing report into a {@code 200}.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
    void shouldReturnHttpNotFound_whenTheReportVanishesBeforeItsFindingsAreRead() {
      var calculationId = UUID.fromString(
        "9b6e2f41-5c7d-4a08-8e3b-0d1a7c2f4b95"
      );
      doReturn(Optional.of(PASSED))
        .when(reportServiceMock)
        .findReportStatusByCalculationId(calculationId);
      doReturn(Optional.empty())
        .when(reportServiceMock)
        .findReportWithFindingsByCalculationId(calculationId);

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseIsNotFound(response, calculationId);
      verifyNoInteractions(qualityGateReportMapperMock);
    }

    /** The same race on the in-progress side, where the {@code 202} body is read separately. */
    @Test
    @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
    void shouldReturnHttpNotFound_whenTheReportVanishesBeforeTheAcceptedBodyIsRead() {
      var calculationId = UUID.fromString(
        "4c8b1d3e-7a52-4e19-b0d6-2f9e5a1c7b38"
      );
      doReturn(Optional.of(IN_PROGRESS))
        .when(reportServiceMock)
        .findReportStatusByCalculationId(calculationId);
      doReturn(Optional.empty())
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseIsNotFound(response, calculationId);
      verifyNoInteractions(qualityGateReportMapperMock);
    }

    /**
     * The projection selected the in-progress branch, but the report read after it has already
     * completed: answering {@code 202} here would ship an {@code Accepted} whose body says
     * {@code PASSED}, so the report that is about to be serialized settles the status.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
    @VerifiesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
    void shouldReturnFindings_whenTheReportCompletesBetweenTheStatusAndTheBody() {
      var calculationId = UUID.fromString(
        "1d7f4a90-3b62-4c8e-9f15-8a2c6e0b4d73"
      );
      doReturn(Optional.of(IN_PROGRESS))
        .when(reportServiceMock)
        .findReportStatusByCalculationId(calculationId);
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);
      doReturn(PASSED).when(qualityGateReport).getReportStatus();

      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportWithFindingsByCalculationId(calculationId);
      var responseDto = mock(GetReportByCalculationId200Response.class);
      doReturn(responseDto)
        .when(qualityGateReportMapperMock)
        .toReportDto(qualityGateReport);

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseHasBody(response, OK, responseDto);

      verify(qualityGateReportMapperMock, never()).toListDto(any());
    }

    @Test
    @VerifiesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
    void shouldServeFindingsWithoutASecondRequest() {
      var calculationId = UUID.fromString(
        "f0a3c6d2-0c29-4a1e-9a1b-63f8e6d6a9b1"
      );
      var responseDto = configureServiceMock(calculationId, PASSED);

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseHasBody(response, OK, responseDto);

      // The findings ride along in the body the drilldown already asked for: a completed report is
      // mapped by 'toReportDto', so nothing the client does next has to fetch them.
      verify(qualityGateReportMapperMock).toReportDto(qualityGateReport);
      verifyNoMoreInteractions(qualityGateReportMapperMock);
    }

    private void assertThatResponseIsStatusOkWithDto(
      ReportStatus reportStatus,
      UUID calculationId
    ) {
      var responseDto = configureServiceMock(calculationId, reportStatus);

      var response = fixture.getReportByCalculationId(calculationId);

      assertThatResponseHasBody(response, OK, responseDto);
    }

    private static void assertThatResponseHasBody(
      ResponseEntity response,
      HttpStatus ok,
      Object responseDto
    ) {
      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(ok),
          r -> assertThat(r.getBody()).isEqualTo(responseDto)
        );
    }

    private static void assertThatResponseIsNotFound(
      ResponseEntity response,
      UUID calculationId
    ) {
      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(NOT_FOUND),
          r ->
            assertThat(r.getBody())
              .asInstanceOf(type(ListQualityGateReports500Response.class))
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
  }

  @Nested
  class GetReportByCalculationIdAsJUnitTest {

    @Mock
    private QualityGateReport qualityGateReport;

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    void shouldReadTheReportWithoutItsFindings() {
      var calculationId = UUID.fromString(
        "3d8a1c77-3b0f-4a27-9f2e-1c9d0b5a7e42"
      );
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);
      doReturn(mock(TestSuites.class))
        .when(jUnitReporterMock)
        .transformToJUnitTestSuites(qualityGateReport);

      fixture.getReportByCalculationIdAsJUnit(calculationId);

      // A CI poll renders XML and never renders evidence; loading the grandchild table for it
      // would spend a join per result on a body nobody reads.
      verify(reportServiceMock, never()).findReportWithFindingsByCalculationId(
        any()
      );
    }

    @Test
    void shouldReturnJUnitReport() {
      var calculationId = UUID.fromString(
        "81699bec-99a0-4c8f-a9d0-06729477fe00"
      );
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);

      var testSuitesMock = mock(TestSuites.class);
      doReturn(testSuitesMock)
        .when(jUnitReporterMock)
        .transformToJUnitTestSuites(qualityGateReport);

      var response = fixture.getReportByCalculationIdAsJUnit(calculationId);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r ->
            assertThat(r.getHeaders().toSingleValueMap())
              .hasSize(2)
              .containsEntry(
                CONTENT_DISPOSITION,
                "attachment; filename=\"snow-white-junit.xml\""
              )
              .containsEntry(CONTENT_TYPE, APPLICATION_XML_VALUE),
          r -> assertThat(r.getBody()).isEqualTo(testSuitesMock)
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
    void shouldReturnAcceptedAsJson_whenReportIsStillInProgress() {
      var calculationId = UUID.fromString(
        "2a4d1e0b-0b4a-4d0e-9a3f-7b8c5d6e1f20"
      );
      doReturn(Optional.of(qualityGateReport))
        .when(reportServiceMock)
        .findReportByCalculationId(calculationId);
      doReturn(IN_PROGRESS).when(qualityGateReport).getReportStatus();

      var responseDto = mock(ListQualityGateReports200ResponseInner.class);
      doReturn(responseDto)
        .when(qualityGateReportMapperMock)
        .toListDto(qualityGateReport);

      var response = fixture.getReportByCalculationIdAsJUnit(calculationId);

      // A half-populated JUnit document would read as a genuine passing test run,
      // so the in-progress answer stays JSON even on the XML endpoint.
      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(ACCEPTED),
          r ->
            assertThat(r.getHeaders().toSingleValueMap()).containsEntry(
              CONTENT_TYPE,
              APPLICATION_JSON_VALUE
            ),
          r -> assertThat(r.getBody()).isEqualTo(responseDto)
        );

      verifyNoInteractions(jUnitReporterMock);
    }

    @Test
    @VerifiesSw(SwTraceables.SW_014_IN_PROGRESS_REPORT_ANSWERS_ACCEPTED)
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
              .asInstanceOf(type(ListQualityGateReports500Response.class))
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
  }

  @Nested
  class ListQualityGateReportsTest {

    @Test
    void shouldReturnListOfQualityGateReports() {
      var page = 0;
      var size = 10;
      var sort = "initiatedAt,asc";

      var report1 = mock(QualityGateReport.class);
      var report2 = mock(QualityGateReport.class);

      Page<@NonNull QualityGateReport> qualityGateReportsPage = mock();
      doReturn(2L).when(qualityGateReportsPage).getTotalElements();

      ArgumentCaptor<Pageable> pageableArgumentCaptor = captor();
      doReturn(qualityGateReportsPage)
        .when(reportServiceMock)
        .findAllReports(
          isNull(),
          isNull(),
          isNull(),
          pageableArgumentCaptor.capture()
        );

      doReturn(Stream.of(report1, report2))
        .when(qualityGateReportsPage)
        .stream();

      var dto1 = mock(ListQualityGateReports200ResponseInner.class);
      doReturn(dto1).when(qualityGateReportMapperMock).toListDto(report1);

      var dto2 = mock(ListQualityGateReports200ResponseInner.class);
      doReturn(dto2).when(qualityGateReportMapperMock).toListDto(report2);

      ResponseEntity<
        @NonNull List<ListQualityGateReports200ResponseInner>
      > response = fixture.listQualityGateReports(
        page,
        size,
        sort,
        null,
        null,
        null
      );

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).containsExactly(dto1, dto2),
          r ->
            assertThat(r.getHeaders().toSingleValueMap())
              .hasSize(1)
              .containsEntry(HEADER_X_TOTAL_COUNT, "2")
        );

      assertThat(pageableArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          p -> assertThat(p.getPageNumber()).isEqualTo(page),
          p -> assertThat(p.getOffset()).isEqualTo(page),
          p -> assertThat(p.getPageSize()).isEqualTo(size),
          p ->
            assertThat(p.getSort()).isEqualTo(
              Sort.by(Sort.Direction.ASC, "createdAt").and(
                Sort.by(Sort.Direction.ASC, "calculationId")
              )
            )
        );
    }

    @Test
    void shouldHandleEmptyListOfQualityGateReports() {
      var page = 1;
      var size = 10;
      var sort = "initiatedAt,desc";

      Page<@NonNull QualityGateReport> qualityGateReportsPage = mock();

      ArgumentCaptor<Pageable> pageableArgumentCaptor = captor();
      doReturn(qualityGateReportsPage)
        .when(reportServiceMock)
        .findAllReports(
          isNull(),
          isNull(),
          isNull(),
          pageableArgumentCaptor.capture()
        );

      doReturn(Stream.empty()).when(qualityGateReportsPage).stream();

      ResponseEntity<
        @NonNull List<ListQualityGateReports200ResponseInner>
      > response = fixture.listQualityGateReports(
        page,
        size,
        sort,
        null,
        null,
        null
      );

      verifyNoInteractions(qualityGateReportMapperMock);

      assertThat(response)
        .isNotNull()
        .satisfies(
          r -> assertThat(r.getStatusCode()).isEqualTo(OK),
          r -> assertThat(r.getBody()).isEmpty(),
          r ->
            assertThat(r.getHeaders().toSingleValueMap())
              .hasSize(1)
              .containsEntry(HEADER_X_TOTAL_COUNT, "0")
        );

      assertThat(pageableArgumentCaptor.getValue())
        .isNotNull()
        .satisfies(
          p -> assertThat(p.getPageNumber()).isEqualTo(page),
          p -> assertThat(p.getOffset()).isEqualTo(page * size),
          p -> assertThat(p.getPageSize()).isEqualTo(size),
          p ->
            assertThat(p.getSort()).isEqualTo(
              Sort.by(Sort.Direction.DESC, "createdAt").and(
                Sort.by(Sort.Direction.ASC, "calculationId")
              )
            )
        );
    }

    @Test
    void shouldPassFiltersToService_whenFiltersProvided() {
      Page<@NonNull QualityGateReport> qualityGateReportsPage = mock();
      doReturn(0L).when(qualityGateReportsPage).getTotalElements();
      doReturn(Stream.empty()).when(qualityGateReportsPage).stream();

      doReturn(qualityGateReportsPage)
        .when(reportServiceMock)
        .findAllReports(
          eq("my-service"),
          eq("my-api"),
          eq("v1"),
          any(Pageable.class)
        );

      fixture.listQualityGateReports(
        0,
        10,
        "initiatedAt,asc",
        "my-service",
        "my-api",
        "v1"
      );

      verify(reportServiceMock).findAllReports(
        eq("my-service"),
        eq("my-api"),
        eq("v1"),
        any(Pageable.class)
      );
    }
  }
}

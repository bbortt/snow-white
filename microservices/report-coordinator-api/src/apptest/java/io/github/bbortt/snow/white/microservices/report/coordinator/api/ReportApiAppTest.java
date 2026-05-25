/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.CitrusUtils.getHttpEndpoint;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports500Response;
import java.util.List;
import java.util.UUID;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.report.api.ReportApi;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@CitrusSupport
class ReportApiAppTest {

  private static ReportApi reportApi;

  @BeforeAll
  static void beforeAllSetup() {
    reportApi = new ReportApi(
      getHttpEndpoint(
        getProperty("report-coordinator-api.host", "localhost"),
        parseInt(getProperty("report-coordinator-api.port", "8084"))
      )
    );
  }

  /**
   * Verifies that the reports listing endpoint accepts pagination parameters and returns HTTP 200.
   *
   * <p>
   * When requesting {@code GET /api/rest/v1/reports?page=0&size=5}, the service must:
   * <ul>
   *   <li>return HTTP 200 OK</li>
   *   <li>return a valid JSON array (empty or populated)</li>
   * </ul>
   *
   * <p>
   * Note: the {@code sort} parameter is intentionally omitted because Citrus currently attempts to
   * parse its value as JSON, causing the request to fail.
   */
  @Test
  @CitrusTest
  void shouldListQualityGateReportsWithPaginationParameters(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(reportApi.sendListQualityGateReports().page(0).size(5));

    testRunner.then(
      reportApi
        .receiveListQualityGateReports(OK)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var reports = JsonMapper.shared().readValue(
            payload,
            new TypeReference<List<ListQualityGateReports200ResponseInner>>() {}
          );
          assertThat(reports).isNotNull();
        })
    );
  }

  /**
   * Verifies that requesting a report by an unknown calculation ID returns HTTP 404.
   *
   * <p>
   * When sending {@code GET /api/rest/v1/reports/{randomUUID}}, the service must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenReportNotFound(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var calculationId = randomUUID();
    testRunner.when(reportApi.sendGetReportByCalculationId(calculationId));

    testRunner.then(
      reportApi
        .receiveGetReportByCalculationId(NOT_FOUND)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var error = JsonMapper.shared().readValue(
            payload,
            ListQualityGateReports500Response.class
          );
          assertThat(error.getCode()).isEqualTo(NOT_FOUND.getReasonPhrase());
          assertThat(error.getMessage()).contains(
            format("No report by id '%s' exists!", calculationId)
          );
        })
    );
  }

  /**
   * Verifies that requesting a JUnit report by an unknown calculation ID returns HTTP 404.
   *
   * <p>
   * When sending {@code GET /api/rest/v1/reports/{randomUUID}/junit}, the service must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenJUnitReportNotFound(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var calculationId = randomUUID();
    testRunner.when(
      reportApi.sendGetReportByCalculationIdAsJUnit(calculationId)
    );

    testRunner.then(
      reportApi
        .receiveGetReportByCalculationIdAsJUnit(NOT_FOUND)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var error = JsonMapper.shared().readValue(
            payload,
            ListQualityGateReports500Response.class
          );
          assertThat(error.getCode()).isEqualTo(NOT_FOUND.getReasonPhrase());
          assertThat(error.getMessage()).contains(
            format("No report by id '%s' exists!", calculationId)
          );
        })
    );
  }
}

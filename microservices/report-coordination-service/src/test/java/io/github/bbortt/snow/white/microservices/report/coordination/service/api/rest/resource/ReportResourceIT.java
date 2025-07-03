/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.StreamUtils.copyToString;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class ReportResourceIT extends AbstractReportCoordinationServiceIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/reports";
  private static final String SINGLE_ENTITY_API_URL =
    ENTITY_API_URL + "/{calculationId}";
  private static final String JUNIT_REPORT_API_URL =
    ENTITY_API_URL + "/{calculationId}/junit";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private MockMvc mockMvc;

  @AfterEach
  void afterEachTeardown() {
    qualityGateReportRepository.deleteAll();
  }

  @Test
  void findReport_IN_PROGRESS_byCalculationId() throws Exception {
    var calculationId = UUID.fromString("3130fae9-e67c-43cd-9c2d-23aee9920736");

    var serviceName = "serviceName";
    var apiName = "apiName";
    var apiVersion = "apiVersion";
    var lookbackWindow = "1m";

    qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName("qualityGateConfigName")
        .reportParameters(
          ReportParameters.builder()
            .serviceName(serviceName)
            .apiName(apiName)
            .apiVersion(apiVersion)
            .lookbackWindow(lookbackWindow)
            .build()
        )
        .reportStatus(IN_PROGRESS)
        .build()
    );

    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, calculationId))
      .andExpect(status().isAccepted())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(jsonPath("$.calculationId").value(calculationId.toString()))
      .andExpect(jsonPath("$.status").value(IN_PROGRESS.name()))
      .andExpect(
        jsonPath("$.calculationRequest.serviceName").value(serviceName)
      )
      .andExpect(jsonPath("$.calculationRequest.apiName").value(apiName))
      .andExpect(jsonPath("$.calculationRequest.apiVersion").value(apiVersion))
      .andExpect(
        jsonPath("$.calculationRequest.lookbackWindow").value(lookbackWindow)
      )
      .andExpect(jsonPath("$.initiatedAt").value(not(nullValue())))
      .andExpect(jsonPath("$.openApiTestResults").isArray());
  }

  @Test
  void findReport_withOpenApiResults_byCalculationId() throws Exception {
    var calculationId = UUID.fromString("3130fae9-e67c-43cd-9c2d-23aee9920736");

    var serviceName = "serviceName";
    var apiName = "apiName";
    var apiVersion = "apiVersion";
    var lookbackWindow = "1m";

    var qualityGateReport = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName("qualityGateConfigName")
        .reportParameters(
          ReportParameters.builder()
            .serviceName(serviceName)
            .apiName(apiName)
            .apiVersion(apiVersion)
            .lookbackWindow(lookbackWindow)
            .build()
        )
        .reportStatus(FAILED)
        .build()
    );

    var coverage = BigDecimal.valueOf(0.5).setScale(2, HALF_UP);
    var additionalInformation = "some additional information";

    var openApiCriterionResult = OpenApiTestResult.builder()
      .openApiTestCriteria(PATH_COVERAGE.name())
      .qualityGateReport(qualityGateReport)
      .coverage(coverage)
      .includedInReport(TRUE)
      .duration(Duration.ofSeconds(1))
      .additionalInformation(additionalInformation)
      .build();

    qualityGateReportRepository.save(
      qualityGateReport.withOpenApiTestResults(Set.of(openApiCriterionResult))
    );

    var responseAsString = mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, calculationId))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var resultingQualityGateReport = objectMapper.readValue(
      responseAsString,
      ListQualityGateReports200ResponseInner.class
    );

    assertThat(resultingQualityGateReport).satisfies(
        report ->
          assertThat(report.getCalculationId()).isEqualTo(calculationId),
        report ->
          assertThat(report.getStatus()).isEqualTo(
            ListQualityGateReports200ResponseInner.StatusEnum.FAILED
          ),
        report ->
          assertThat(report.getCalculationRequest())
            .isNotNull()
            .satisfies(
              request ->
                assertThat(request.getServiceName()).isEqualTo(serviceName),
              request -> assertThat(request.getApiName()).isEqualTo(apiName),
              request ->
                assertThat(request.getApiVersion()).isEqualTo(apiVersion),
              request ->
                assertThat(request.getLookbackWindow()).isEqualTo(
                  lookbackWindow
                )
            ),
        report -> assertThat(report.getInitiatedAt()).isNotNull(),
        report ->
          assertThat(report.getOpenApiTestResults())
            .hasSize(1)
            .first()
            .satisfies(
              result ->
                assertThat(result.getId()).isEqualTo(PATH_COVERAGE.name()),
              result -> assertThat(result.getCoverage()).isEqualTo(coverage),
              result ->
                assertThat(result.getAdditionalInformation()).isEqualTo(
                  additionalInformation
                )
            )
      );
  }

  @Test
  void findReport_withoutRequiredCalculationId() throws Exception {
    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, "not-a-uuid"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void findReport_withOpenApiResults_byCalculationId_andReceiveJUnitReport()
    throws Exception {
    var calculationId = UUID.fromString("aaac28e5-2d0e-4ea6-8fef-4dc85169759e");

    var qualityGateReport = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName("qualityGateConfigName")
        .reportParameters(
          ReportParameters.builder()
            .serviceName("serviceName")
            .apiName("apiName")
            .build()
        )
        .reportStatus(PASSED)
        .createdAt(Instant.parse("2025-04-28T08:00:00.00Z"))
        .build()
    );

    var openApiCriterionResult = OpenApiTestResult.builder()
      .openApiTestCriteria(PATH_COVERAGE.name())
      .qualityGateReport(qualityGateReport)
      .coverage(ONE)
      .includedInReport(TRUE)
      .duration(Duration.ofSeconds(1))
      .build();

    qualityGateReportRepository.save(
      qualityGateReport.withOpenApiTestResults(Set.of(openApiCriterionResult))
    );

    var jUnitReport = mockMvc
      .perform(get(JUNIT_REPORT_API_URL, calculationId))
      .andExpect(status().isOk())
      .andExpect(
        header().string(
          CONTENT_DISPOSITION,
          "attachment; filename=\"snow-white-junit.xml\""
        )
      )
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_XML_VALUE))
      .andReturn()
      .getResponse()
      .getContentAsString();

    assertXMLEqual(
      copyToString(
        getClass()
          .getClassLoader()
          .getResourceAsStream("ReportResourceIT/JUnitReport.xml"),
        UTF_8
      ),
      jUnitReport
    );
  }

  @Test
  void findAllReports() throws Exception {
    qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(UUID.fromString("b30bb84b-7bf6-4744-8bfc-ac05b8a85991"))
        .qualityGateConfigName("nameA")
        .reportParameters(
          ReportParameters.builder()
            .serviceName("serviceName")
            .apiName("apiName")
            .build()
        )
        .reportStatus(PASSED)
        .createdAt(Instant.parse("2025-05-07T18:00:00.00Z"))
        .build()
    );

    qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(UUID.fromString("99635525-27a5-43ee-ae46-1cedf2ba4c35"))
        .qualityGateConfigName("nameB")
        .reportParameters(
          ReportParameters.builder()
            .serviceName("serviceName")
            .apiName("apiName")
            .build()
        )
        .reportStatus(PASSED)
        .createdAt(Instant.parse("2025-05-07T18:05:00.00Z"))
        .build()
    );

    mockMvc
      .perform(get(ENTITY_API_URL).queryParam("sort", "createdAt,desc"))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "2"))
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].qualityGateConfigName").value("nameB"))
      .andExpect(jsonPath("$[1].qualityGateConfigName").value("nameA"));
  }
}

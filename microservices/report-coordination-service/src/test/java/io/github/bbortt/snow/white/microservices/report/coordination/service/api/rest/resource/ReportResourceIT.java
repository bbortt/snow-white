/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static java.lang.Boolean.TRUE;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.OpenApiCriterionResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameters;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.OpenApiCriterionRepository;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class ReportResourceIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/reports";
  private static final String SINGLE_ENTITY_API_URL =
    ENTITY_API_URL + "/{calculationId}";

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private OpenApiCriterionRepository openApiCriterionRepository;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private MockMvc mockMvc;

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
      .andExpect(jsonPath("$.openApiCriterionResults").isArray());
  }

  @Test
  void findReport_withOpenApiResults_byCalculationId() throws Exception {
    var openApiCriterion = openApiCriterionRepository.save(
      OpenApiCriterion.builder().name(PATH_COVERAGE.name()).build()
    );

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

    var openApiCriterionResult = OpenApiCriterionResult.builder()
      .openApiCriterion(openApiCriterion)
      .qualityGateReport(qualityGateReport)
      .coverage(coverage)
      .includedInReport(TRUE)
      .duration(Duration.ofSeconds(1))
      .additionalInformation(additionalInformation)
      .build();

    qualityGateReport = qualityGateReport.withOpenApiCriterionResults(
      Set.of(openApiCriterionResult)
    );
    qualityGateReportRepository.save(qualityGateReport);

    var responseAsString = mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, calculationId))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    var resultingQualityGateReport = objectMapper.readValue(
      responseAsString,
      io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.class
    );

    assertThat(resultingQualityGateReport).satisfies(
      report -> assertThat(report.getCalculationId()).isEqualTo(calculationId),
      report ->
        assertThat(report.getStatus()).isEqualTo(
          io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.QualityGateReport.StatusEnum.FAILED
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
              assertThat(request.getLookbackWindow()).isEqualTo(lookbackWindow)
          ),
      report -> assertThat(report.getInitiatedAt()).isNotNull(),
      report ->
        assertThat(report.getOpenApiCriterionResults())
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
}

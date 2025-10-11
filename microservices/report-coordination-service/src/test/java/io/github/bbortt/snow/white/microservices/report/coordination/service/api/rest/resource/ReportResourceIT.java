/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.resource;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.CalculateQualityGate202ResponseInterfacesInner.ApiTypeEnum.UNSPECIFIED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.NOT_STARTED;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus.PASSED;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
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

import io.github.bbortt.snow.white.microservices.report.coordination.service.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.repository.QualityGateReportRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@AutoConfigureMockMvc
class ReportResourceIT extends AbstractReportCoordinationServiceIT {

  private static final String ENTITY_API_URL = "/api/rest/v1/reports";
  private static final String SINGLE_ENTITY_API_URL =
    ENTITY_API_URL + "/{calculationId}";
  private static final String JUNIT_REPORT_API_URL =
    ENTITY_API_URL + "/{calculationId}/junit";

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private ApiTestRepository apiTestRepository;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private MockMvc mockMvc;

  private static String reportStatusAsString(ReportStatus reportStatus) {
    return NOT_STARTED.equals(reportStatus)
      ? "IN_PROGRESS"
      : reportStatus.toString();
  }

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

    var qualityGateReport = createAndPersistQualityGateReport(
      calculationId,
      serviceName,
      apiName,
      apiVersion,
      lookbackWindow,
      IN_PROGRESS
    );

    mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, calculationId))
      .andExpect(status().isAccepted())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(jsonPath("$.calculationId").value(calculationId.toString()))
      .andExpect(
        jsonPath("$.qualityGateConfigName").value(
          qualityGateReport.getQualityGateConfigName()
        )
      )
      .andExpect(jsonPath("$.status").value(IN_PROGRESS.name()))
      .andExpect(jsonPath("$.calculationRequest.includeApis.length()").value(1))
      .andExpect(
        jsonPath("$.calculationRequest.includeApis[0].serviceName").value(
          serviceName
        )
      )
      .andExpect(
        jsonPath("$.calculationRequest.includeApis[0].apiName").value(apiName)
      )
      .andExpect(
        jsonPath("$.calculationRequest.includeApis[0].apiVersion").value(
          apiVersion
        )
      )
      .andExpect(
        jsonPath("$.calculationRequest.lookbackWindow").value(lookbackWindow)
      )
      .andExpect(jsonPath("$.initiatedAt").value(not(nullValue())))
      .andExpect(jsonPath("$.interfaces.length()").value(1))
      .andExpect(jsonPath("$.interfaces[0].serviceName").value(serviceName))
      .andExpect(jsonPath("$.interfaces[0].apiName").value(apiName))
      .andExpect(jsonPath("$.interfaces[0].apiVersion").value(apiVersion))
      .andExpect(jsonPath("$.interfaces[0].apiType").value(UNSPECIFIED.name()))
      .andExpect(jsonPath("$.interfaces[0].testResults").isArray());
  }

  @Test
  void findReport_withOpenApiResults_byCalculationId() throws Exception {
    var calculationId = UUID.fromString("3130fae9-e67c-43cd-9c2d-23aee9920736");

    var serviceName = "serviceName";
    var apiName = "apiName";
    var apiVersion = "apiVersion";
    var lookbackWindow = "1m";

    var qualityGateReport = createAndPersistQualityGateReport(
      calculationId,
      serviceName,
      apiName,
      apiVersion,
      lookbackWindow,
      FAILED
    );

    var coverage = BigDecimal.valueOf(0.5).setScale(2, HALF_UP);
    var additionalInformation = "some additional information";

    qualityGateReport = qualityGateReport.withApiTests(
      qualityGateReport
        .getApiTests()
        .stream()
        .map(apiTest ->
          apiTest.withApiTestResults(
            Set.of(
              ApiTestResult.builder()
                .apiTestCriteria(PATH_COVERAGE.name())
                .coverage(coverage)
                .includedInReport(TRUE)
                .duration(Duration.ofSeconds(1))
                .additionalInformation(additionalInformation)
                .apiTest(apiTest)
                .build()
            )
          )
        )
        .collect(toSet())
    );

    qualityGateReportRepository.save(qualityGateReport);

    var responseAsString = mockMvc
      .perform(get(SINGLE_ENTITY_API_URL, calculationId))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var resultingQualityGateReport = jsonMapper.readValue(
      responseAsString,
      ListQualityGateReports200ResponseInner.class
    );

    assertThat(resultingQualityGateReport).satisfies(
      report -> assertThat(report.getCalculationId()).isEqualTo(calculationId),
      report ->
        assertThat(report.getStatus()).isEqualTo(
          ListQualityGateReports200ResponseInner.StatusEnum.FAILED
        ),
      report ->
        assertThat(report.getCalculationRequest())
          .isNotNull()
          .satisfies(
            request ->
              assertThat(request.getIncludeApis())
                .hasSize(1)
                .first()
                .satisfies(
                  includedApi ->
                    assertThat(includedApi.getServiceName()).isEqualTo(
                      serviceName
                    ),
                  includedApi ->
                    assertThat(includedApi.getApiName()).isEqualTo(apiName),
                  includedApi ->
                    assertThat(includedApi.getApiVersion()).isEqualTo(
                      apiVersion
                    )
                ),
            request ->
              assertThat(request.getLookbackWindow()).isEqualTo(lookbackWindow)
          ),
      report -> assertThat(report.getInitiatedAt()).isNotNull(),
      report ->
        assertThat(report.getInterfaces())
          .hasSize(1)
          .first()
          .satisfies(api ->
            assertThat(api).satisfies(
              includedApi ->
                assertThat(includedApi.getServiceName()).isEqualTo(serviceName),
              includedApi ->
                assertThat(includedApi.getApiName()).isEqualTo(apiName),
              includedApi ->
                assertThat(includedApi.getApiVersion()).isEqualTo(apiVersion),
              includedApi ->
                assertThat(includedApi.getTestResults())
                  .hasSize(1)
                  .first()
                  .satisfies(
                    result ->
                      assertThat(result.getId()).isEqualTo(
                        PATH_COVERAGE.name()
                      ),
                    result ->
                      assertThat(result.getCoverage()).isEqualTo(coverage),
                    result ->
                      assertThat(result.getAdditionalInformation()).isEqualTo(
                        additionalInformation
                      ),
                    result ->
                      assertThat(result.getIsIncludedInQualityGate()).isTrue()
                  )
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
        .reportParameter(
          ReportParameter.builder().calculationId(calculationId).build()
        )
        .reportStatus(PASSED)
        .createdAt(Instant.parse("2025-04-28T08:00:00.00Z"))
        .build()
    );

    createSimpleApiTestSet(qualityGateReport);

    qualityGateReport = qualityGateReport.withApiTests(
      qualityGateReport
        .getApiTests()
        .stream()
        .map(apiTest ->
          apiTest.withApiTestResults(
            Set.of(
              ApiTestResult.builder()
                .apiTestCriteria(PATH_COVERAGE.name())
                .coverage(ONE)
                .includedInReport(TRUE)
                .duration(Duration.ofSeconds(1))
                .apiTest(apiTest)
                .build()
            )
          )
        )
        .collect(toSet())
    );

    qualityGateReportRepository.save(qualityGateReport);

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

  @ParameterizedTest
  @EnumSource(ReportStatus.class)
  void findAllReports(ReportStatus reportStatus) throws Exception {
    var calculationId1 = UUID.fromString(
      "b30bb84b-7bf6-4744-8bfc-ac05b8a85991"
    );
    var qualityGateReport1 = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId1)
        .qualityGateConfigName("nameA")
        .reportParameter(
          ReportParameter.builder().calculationId(calculationId1).build()
        )
        .reportStatus(reportStatus)
        .createdAt(Instant.parse("2025-05-07T18:00:00.00Z"))
        .build()
    );

    var calculationId2 = UUID.fromString(
      "99635525-27a5-43ee-ae46-1cedf2ba4c35"
    );
    var qualityGateReport2 = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId2)
        .qualityGateConfigName("nameB")
        .reportParameter(
          ReportParameter.builder().calculationId(calculationId2).build()
        )
        .reportStatus(reportStatus)
        .createdAt(Instant.parse("2025-05-07T18:05:00.00Z"))
        .build()
    );

    createSimpleApiTestSet(qualityGateReport1);
    createSimpleApiTestSet(qualityGateReport2);

    mockMvc
      .perform(get(ENTITY_API_URL).queryParam("sort", "createdAt,desc"))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "2"))
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].qualityGateConfigName").value("nameB"))
      .andExpect(
        jsonPath("$[0].status").value(reportStatusAsString(reportStatus))
      )
      .andExpect(jsonPath("$[1].qualityGateConfigName").value("nameA"))
      .andExpect(
        jsonPath("$[1].status").value(reportStatusAsString(reportStatus))
      );
  }

  private QualityGateReport createAndPersistQualityGateReport(
    UUID calculationId,
    String serviceName,
    String apiName,
    String apiVersion,
    String lookbackWindow,
    ReportStatus reportStatus
  ) {
    var qualityGateReport = QualityGateReport.builder()
      .calculationId(calculationId)
      .qualityGateConfigName("qualityGateConfigName")
      .reportParameter(
        ReportParameter.builder()
          .calculationId(calculationId)
          .lookbackWindow(lookbackWindow)
          .build()
      )
      .reportStatus(reportStatus)
      .build();

    final var persistedQualityGateReport = qualityGateReportRepository.save(
      qualityGateReport
    );

    var persistedApiTests = apiTestRepository.save(
      ApiTest.builder()
        .serviceName(serviceName)
        .apiName(apiName)
        .apiVersion(apiVersion)
        .qualityGateReport(persistedQualityGateReport)
        .build()
    );

    return persistedQualityGateReport.withApiTests(Set.of(persistedApiTests));
  }

  private void createSimpleApiTestSet(QualityGateReport qualityGateReport) {
    apiTestRepository.save(
      ApiTest.builder()
        .serviceName("serviceName")
        .apiName("apiName")
        .qualityGateReport(qualityGateReport)
        .build()
    );
  }
}

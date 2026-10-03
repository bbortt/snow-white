/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.resource;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.UNSPECIFIED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.commons.web.PaginationUtils.HEADER_X_TOTAL_COUNT;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.ReportApi.*;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.*;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.util.StreamUtils.copyToString;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.GetReportByCalculationId200Response;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.rest.dto.ListQualityGateReports200ResponseInner;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.*;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@AutoConfigureMockMvc
class ReportResourceIT extends AbstractReportCoordinationServiceIT {

  private static final String COVERED_SPEC_POINTER =
    "/paths/~1api~1v1~1users/get/responses/404";
  private static final String UNCOVERED_SPEC_POINTER =
    "/paths/~1api~1v1~1users/get/responses/500";

  private static final String TRACE_ID_WITHOUT_TEST_IDENTITY =
    "1f8b0c4d2e3a4b5c6d7e8f9a0b1c2d3e";
  private static final String TRACE_ID_WITH_TEST_IDENTITY =
    "2e3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c";

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private ApiTestRepository apiTestRepository;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private MockMvc mockMvc;

  private static String reportStatusAsString(
    @NonNull ReportStatus reportStatus
  ) {
    return NOT_STARTED.equals(reportStatus)
      ? "IN_PROGRESS"
      : reportStatus.toString();
  }

  /**
   * The database outlives the test class, and the listener ITs leave their reports behind. A list
   * read asserting on the whole page needs an empty table regardless of which class ran before it,
   * so the teardown is mirrored here rather than relied on.
   */
  @BeforeEach
  void beforeEachCleanSlate() {
    qualityGateReportRepository.deleteAll();
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
      .perform(get(PATH_GET_REPORT_BY_CALCULATION_ID, calculationId))
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
      .perform(get(PATH_GET_REPORT_BY_CALCULATION_ID, calculationId))
      .andExpect(status().isOk())
      .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
      .andReturn()
      .getResponse()
      .getContentAsString();

    var resultingQualityGateReport = jsonMapper.readValue(
      responseAsString,
      GetReportByCalculationId200Response.class
    );

    assertThat(resultingQualityGateReport).satisfies(
      report -> assertThat(report.getCalculationId()).isEqualTo(calculationId),
      report ->
        assertThat(report.getStatus()).isEqualTo(
          GetReportByCalculationId200Response.StatusEnum.FAILED
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
  @VerifiesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  @VerifiesCon(ConTraceables.CON_010_REST_RESPONSES_NEVER_CARRY_NULL)
  void findReport_servesTheFindingsBehindEveryCriterionResult()
    throws Exception {
    var calculationId = UUID.fromString("52ad2f1b-2a9e-4d42-9e2f-9c2a0f9f7a21");

    persistReportWithFindings(calculationId);

    var findingsArray = readSingleTestResult(calculationId).get("findings");

    // Ordered by spec pointer, and asserted in order: the entity's identity hashCode would
    // otherwise let two identical requests hand the same findings over in two different orders.
    assertThat(valuesOf(findingsArray, "specPointer")).containsExactly(
      COVERED_SPEC_POINTER,
      UNCOVERED_SPEC_POINTER
    );

    var findings = indexBy(findingsArray, "specPointer");

    var covered = findings.get(COVERED_SPEC_POINTER);
    // The status reads as its name; the stored code says nothing to whoever opens the drilldown.
    assertThat(covered.get("status").asString()).isEqualTo("COVERED");
    assertThat(covered.get("httpPath").asString()).isEqualTo("/api/v1/users");
    assertThat(covered.get("httpMethod").asString()).isEqualTo("GET");
    assertThat(covered.get("responseCode").asString()).isEqualTo("404");
    assertThat(covered.has("parameterName")).isFalse();
    assertThat(covered.get("contentType").asString()).isEqualTo(
      "application/json"
    );
    assertThat(covered.get("evidence").size()).isEqualTo(2);

    var uncovered = findings.get(UNCOVERED_SPEC_POINTER);
    assertThat(uncovered.get("status").asString()).isEqualTo("UNCOVERED");
    // Empty never means "not loaded" - it means this finding has no evidence. Only null is
    // omitted, so the empty array stays on the wire.
    assertThat(uncovered.has("evidence")).isTrue();
    assertThat(uncovered.get("evidence").isEmpty()).isTrue();
  }

  @Test
  @VerifiesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  @VerifiesCon(ConTraceables.CON_010_REST_RESPONSES_NEVER_CARRY_NULL)
  void findReport_servesEvidenceAsObjectsOmittingAnUnsetTestCaseName()
    throws Exception {
    var calculationId = UUID.fromString("7b3f0c58-0a6b-4f95-8d4e-2f5c4a1b6d33");

    persistReportWithFindings(calculationId);

    var evidence = indexBy(
      indexBy(
        readSingleTestResult(calculationId).get("findings"),
        "specPointer"
      )
        .get(COVERED_SPEC_POINTER)
        .get("evidence"),
      "traceId"
    );

    assertThat(evidence).containsOnlyKeys(
      TRACE_ID_WITHOUT_TEST_IDENTITY,
      TRACE_ID_WITH_TEST_IDENTITY
    );

    assertThat(
      evidence.get(TRACE_ID_WITH_TEST_IDENTITY).get("testCaseName").asString()
    ).isEqualTo("shouldReturnNotFound");

    // A trace whose span carried no test identity carries no key at all: an unset property is
    // omitted rather than served as null.
    var withoutTestIdentity = evidence.get(TRACE_ID_WITHOUT_TEST_IDENTITY);
    assertThat(withoutTestIdentity.has("traceId")).isTrue();
    assertThat(withoutTestIdentity.has("testCaseName")).isFalse();
  }

  @Test
  @VerifiesSw(SwTraceables.SW_031_FINDINGS_SERVED_WITH_THE_REPORT)
  void findReport_servesAnEmptyFindingsArray_leavingEveryOtherFieldUntouched()
    throws Exception {
    var calculationId = UUID.fromString("c1f4a6d8-5e2b-4a70-9f13-8b6c0d2e4a55");

    var coverage = BigDecimal.valueOf(0.5).setScale(2, HALF_UP);
    var additionalInformation = "some additional information";

    var qualityGateReport = createAndPersistQualityGateReport(
      calculationId,
      "serviceName",
      "apiName",
      "apiVersion",
      "1m",
      FAILED
    );

    // A report written before findings existed: its results carry none, and nothing about the
    // coverage they already reported may move.
    qualityGateReportRepository.save(
      qualityGateReport.withApiTests(
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
      )
    );

    var testResult = readSingleTestResult(calculationId);

    assertThat(testResult.has("findings")).isTrue();
    assertThat(testResult.get("findings").isEmpty()).isTrue();

    var listedReport = jsonMapper
      .readTree(
        mockMvc
          .perform(get(PATH_LIST_QUALITY_GATE_REPORTS))
          .andExpect(status().isOk())
          .andReturn()
          .getResponse()
          .getContentAsString()
      )
      .get(0);

    // The findings are the only thing the widened shape adds; everything the list read already
    // served must render identically here. Asserted over the whole report rather than the one
    // criterion result, because three components are forked - the report, the interface and the
    // result - and a field added to any narrow one would otherwise vanish from this read with a
    // green build.
    assertThat(withoutFindings(readSingleReport(calculationId))).isEqualTo(
      listedReport
    );
  }

  @Test
  void findReport_withoutRequiredCalculationId() throws Exception {
    mockMvc
      .perform(get(PATH_GET_REPORT_BY_CALCULATION_ID, "not-a-uuid"))
      .andExpect(status().isBadRequest());
  }

  /**
   * All four shapes that answer a report publish the threshold it was scored against. The report is
   * pinned at {@code 85} rather than the domain default of {@code 100}, so a read that hardcoded the
   * default, or dropped the property on one of the two components, fails here instead of passing by
   * coincidence.
   * <p>
   * {@code IN_PROGRESS} is what makes one report reach all four: it answers {@code 202} on both
   * report endpoints, and the completed {@code 200} shape is asserted by the sibling case below.
   */
  @Test
  @VerifiesSw(SwTraceables.SW_039_REPORT_PUBLISHES_ITS_PINNED_THRESHOLD)
  void everyReportReadPublishesTheThresholdTheReportWasScoredAgainst()
    throws Exception {
    var calculationId = UUID.fromString("5f0d4b1e-2c33-4a7e-9f61-6d0a8c2b4e17");

    persistReportPinnedAt(calculationId, 85, IN_PROGRESS);

    mockMvc
      .perform(get(PATH_GET_REPORT_BY_CALCULATION_ID, calculationId))
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.minCoveragePercentage").value(85));

    mockMvc
      .perform(get(PATH_GET_REPORT_BY_CALCULATION_ID_AS_J_UNIT, calculationId))
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.minCoveragePercentage").value(85));

    mockMvc
      .perform(get(PATH_LIST_QUALITY_GATE_REPORTS))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].minCoveragePercentage").value(85));
  }

  /**
   * The findings-carrying {@code 200} publishes the same pinned threshold as the narrow shapes. It
   * is a forked component, so a property added to the shared one alone would vanish from exactly
   * this read with a green build.
   */
  @Test
  @VerifiesSw(SwTraceables.SW_039_REPORT_PUBLISHES_ITS_PINNED_THRESHOLD)
  void theCompletedReportReadPublishesTheSamePinnedThreshold()
    throws Exception {
    var calculationId = UUID.fromString("9c1e7a46-8b52-4d0f-ae33-1b7c05d9e284");

    persistReportPinnedAt(calculationId, 85, PASSED);

    mockMvc
      .perform(get(PATH_GET_REPORT_BY_CALCULATION_ID, calculationId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.minCoveragePercentage").value(85));
  }

  /**
   * The read answers the number pinned on the report, not one the gate currently carries. The column
   * is non-updatable, so an attempt to move the report's threshold after the fact - which is what a
   * later edit to the gate would have to do to change this read - leaves the pinned value standing.
   */
  @Test
  @VerifiesSw(SwTraceables.SW_039_REPORT_PUBLISHES_ITS_PINNED_THRESHOLD)
  void theReadAnswersThePinnedThresholdAfterAnAttemptToMoveIt()
    throws Exception {
    var calculationId = UUID.fromString("2a6f9d08-4e71-4c5b-b3a2-7e8d1f046c93");

    persistReportPinnedAt(calculationId, 85, PASSED);

    qualityGateReportRepository.saveAndFlush(
      qualityGateReportRepository
        .findById(calculationId)
        .orElseThrow()
        .withMinCoveragePercentage(95)
    );

    mockMvc
      .perform(get(PATH_GET_REPORT_BY_CALCULATION_ID, calculationId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.minCoveragePercentage").value(85));
  }

  /** One report pinned at a given threshold, with the one API test every read shape walks. */
  private QualityGateReport persistReportPinnedAt(
    UUID calculationId,
    Integer minCoveragePercentage,
    ReportStatus reportStatus
  ) {
    var qualityGateReport = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName("qualityGateConfigName")
        .minCoveragePercentage(minCoveragePercentage)
        .reportParameter(
          ReportParameter.builder().calculationId(calculationId).build()
        )
        .reportStatus(reportStatus.getVal())
        .createdAt(Instant.parse("2025-05-07T18:05:00.00Z"))
        .build()
    );

    createSimpleApiTestSet(qualityGateReport);

    return qualityGateReport;
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
        .reportStatus(PASSED.getVal())
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

    qualityGateReportRepository.saveAndFlush(qualityGateReport);

    var jUnitReport = mockMvc
      .perform(
        get(PATH_GET_REPORT_BY_CALCULATION_ID_AS_J_UNIT, calculationId).accept(
          APPLICATION_XML_VALUE
        )
      )
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

  @EnumSource
  @ParameterizedTest
  void findAllReports(ReportStatus reportStatus) throws Exception {
    persistTwoReportsOrderedByCreationTime(reportStatus);

    mockMvc
      .perform(
        get(PATH_LIST_QUALITY_GATE_REPORTS).queryParam(
          "sort",
          "initiatedAt,desc"
        )
      )
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

  /**
   * Without a {@code sort} the listing still orders newest first, so a caller that expresses no
   * preference gets the documented default rather than whatever the database returns.
   */
  @Test
  void findAllReportsWithoutSortAppliesTheDocumentedDefaultOrder()
    throws Exception {
    persistTwoReportsOrderedByCreationTime(PASSED);

    mockMvc
      .perform(get(PATH_LIST_QUALITY_GATE_REPORTS))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].qualityGateConfigName").value("nameB"))
      .andExpect(jsonPath("$[1].qualityGateConfigName").value("nameA"));
  }

  /**
   * {@code createdAt} is the entity attribute behind the published {@code initiatedAt}. It is not a
   * second spelling of it, and asking for it is a request this listing refuses rather than a 500.
   */
  @ParameterizedTest
  @ValueSource(
    strings = {
      "createdAt,desc",
      "status,asc",
      "initiatedAt",
      "initiatedAt,sideways",
    }
  )
  void findAllReportsRejectsASortItDoesNotPublish(String sort)
    throws Exception {
    mockMvc
      .perform(get(PATH_LIST_QUALITY_GATE_REPORTS).queryParam("sort", sort))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code").value("Bad Request"))
      .andExpect(
        jsonPath("$.message").value(
          containsString(
            "one of: calculationId, initiatedAt, qualityGateConfigName"
          )
        )
      );
  }

  /** Two reports a known five minutes apart, so any assertion on order has something to order. */
  private void persistTwoReportsOrderedByCreationTime(
    ReportStatus reportStatus
  ) {
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
        .reportStatus(reportStatus.getVal())
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
        .reportStatus(reportStatus.getVal())
        .createdAt(Instant.parse("2025-05-07T18:05:00.00Z"))
        .build()
    );

    createSimpleApiTestSet(qualityGateReport1);
    createSimpleApiTestSet(qualityGateReport2);
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
      .reportStatus(reportStatus.getVal())
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
        .apiType(UNSPECIFIED.getVal())
        .build()
    );

    return persistedQualityGateReport.withApiTests(Set.of(persistedApiTests));
  }

  /**
   * One report, one criterion result, two findings: a covered one carrying every discriminator the
   * target has plus two traces - one of them named, one not - and an uncovered one with no evidence
   * at all.
   */
  private void persistReportWithFindings(UUID calculationId) {
    var apiTest = createAndPersistQualityGateReport(
      calculationId,
      "serviceName",
      "apiName",
      "apiVersion",
      "1m",
      FAILED
    )
      .getApiTests()
      .iterator()
      .next();

    var apiTestResult = ApiTestResult.builder()
      .apiTestCriteria(PATH_COVERAGE.name())
      .coverage(BigDecimal.valueOf(0.5).setScale(2, HALF_UP))
      .includedInReport(TRUE)
      .duration(Duration.ofSeconds(1))
      .additionalInformation("some additional information")
      .apiTest(apiTest)
      .build();

    var covered = ApiTestFinding.builder()
      .specPointer(COVERED_SPEC_POINTER)
      .status(FindingStatus.COVERED.getVal())
      .httpPath("/api/v1/users")
      .httpMethod("GET")
      .responseCode("404")
      .contentType("application/json")
      .apiTestResult(apiTestResult)
      .build();

    covered
      .getEvidence()
      .addAll(
        Set.of(
          FindingEvidence.builder()
            .traceId(TRACE_ID_WITHOUT_TEST_IDENTITY)
            .build(),
          FindingEvidence.builder()
            .traceId(TRACE_ID_WITH_TEST_IDENTITY)
            .testCaseName("shouldReturnNotFound")
            .build()
        )
      );

    var uncovered = ApiTestFinding.builder()
      .specPointer(UNCOVERED_SPEC_POINTER)
      .status(FindingStatus.UNCOVERED.getVal())
      .httpPath("/api/v1/users")
      .httpMethod("GET")
      .responseCode("500")
      .contentType("application/json")
      .apiTestResult(apiTestResult)
      .build();

    apiTestResult.getFindings().addAll(Set.of(covered, uncovered));

    apiTestRepository.save(apiTest.withApiTestResults(Set.of(apiTestResult)));
  }

  /**
   * The report read as a tree rather than through the generated DTO: these tests assert on what is
   * on the wire - a key present and null is not the same as an absent one, and a DTO cannot tell
   * them apart.
   */
  private JsonNode readSingleReport(UUID calculationId) throws Exception {
    return jsonMapper.readTree(
      mockMvc
        .perform(get(PATH_GET_REPORT_BY_CALCULATION_ID, calculationId))
        .andExpect(status().isOk())
        .andExpect(header().string(CONTENT_TYPE, APPLICATION_JSON_VALUE))
        .andReturn()
        .getResponse()
        .getContentAsString()
    );
  }

  private JsonNode readSingleTestResult(UUID calculationId) throws Exception {
    return readSingleReport(calculationId)
      .get("interfaces")
      .get(0)
      .get("testResults")
      .get(0);
  }

  /** The report with {@code findings} stripped from every criterion result it carries. */
  private static JsonNode withoutFindings(JsonNode report) {
    var stripped = report.deepCopy();

    for (var api : stripped.get("interfaces")) {
      for (var testResult : api.get("testResults")) {
        ((ObjectNode) testResult).without("findings");
      }
    }

    return stripped;
  }

  private static Map<String, JsonNode> indexBy(
    JsonNode array,
    String keyField
  ) {
    var byKey = new HashMap<String, JsonNode>();

    for (var element : array) {
      byKey.put(element.get(keyField).asString(), element);
    }

    return byKey;
  }

  private static List<String> valuesOf(JsonNode array, String field) {
    var values = new ArrayList<String>();

    for (var element : array) {
      values.add(element.get(field).asString());
    }

    return values;
  }

  @Test
  void findAllReports_filteredByServiceName_returnsOnlyMatchingReports()
    throws Exception {
    var calculationId1 = UUID.fromString(
      "30f7b64c-2a31-4452-90bc-1210d4a2faab"
    );
    createAndPersistQualityGateReport(
      calculationId1,
      "alpha",
      "api1",
      "v1",
      "1h",
      PASSED
    );

    var calculationId2 = UUID.fromString(
      "84753ea3-3ee3-446b-be96-edb6b049f4a3"
    );
    createAndPersistQualityGateReport(
      calculationId2,
      "beta",
      "api1",
      "v1",
      "1h",
      PASSED
    );

    mockMvc
      .perform(
        get(PATH_LIST_QUALITY_GATE_REPORTS).queryParam("serviceName", "alpha")
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "1"))
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(
        jsonPath("$[0].calculationId").value(calculationId1.toString())
      );
  }

  @Test
  void findAllReports_filteredByApiName_returnsOnlyMatchingReports()
    throws Exception {
    var calculationId1 = UUID.fromString(
      "a785a394-c1ef-492d-b96c-fa6a24ff7e10"
    );
    createAndPersistQualityGateReport(
      calculationId1,
      "svc",
      "foo-api",
      "v1",
      "1h",
      PASSED
    );

    var calculationId2 = UUID.fromString(
      "65fef062-2dfe-47b6-9182-79ede0d37a6f"
    );
    createAndPersistQualityGateReport(
      calculationId2,
      "svc",
      "bar-api",
      "v1",
      "1h",
      PASSED
    );

    mockMvc
      .perform(
        get(PATH_LIST_QUALITY_GATE_REPORTS).queryParam("apiName", "foo-api")
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "1"))
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(
        jsonPath("$[0].calculationId").value(calculationId1.toString())
      );
  }

  @Test
  void findAllReports_filteredByApiVersion_returnsOnlyMatchingReports()
    throws Exception {
    var calculationId1 = UUID.fromString(
      "8f75c75e-c4a2-4a36-af55-70c98565b098"
    );
    createAndPersistQualityGateReport(
      calculationId1,
      "svc",
      "api1",
      "v1",
      "1h",
      PASSED
    );

    var calculationId2 = UUID.fromString(
      "2a4bb6c5-ee0b-4f8d-a3ce-ac5abc24f2be"
    );
    createAndPersistQualityGateReport(
      calculationId2,
      "svc",
      "api1",
      "v2",
      "1h",
      PASSED
    );

    mockMvc
      .perform(
        get(PATH_LIST_QUALITY_GATE_REPORTS).queryParam("apiVersion", "v1")
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "1"))
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(
        jsonPath("$[0].calculationId").value(calculationId1.toString())
      );
  }

  @Test
  void findAllReports_filteredByServiceNameAndApiName_returnsOnlyMatchingReports()
    throws Exception {
    var calculationId1 = UUID.fromString(
      "2e2bafb5-c0cd-435c-8cb9-b91516c7ef3d"
    );
    createAndPersistQualityGateReport(
      calculationId1,
      "alpha",
      "foo-api",
      "v1",
      "1h",
      PASSED
    );

    var calculationId2 = UUID.fromString(
      "3f1ba337-3bf5-46af-89a3-8ddcf199caa7"
    );
    createAndPersistQualityGateReport(
      calculationId2,
      "alpha",
      "bar-api",
      "v1",
      "1h",
      PASSED
    );

    mockMvc
      .perform(
        get(PATH_LIST_QUALITY_GATE_REPORTS)
          .queryParam("serviceName", "alpha")
          .queryParam("apiName", "foo-api")
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "1"))
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(
        jsonPath("$[0].calculationId").value(calculationId1.toString())
      );
  }

  @Test
  void findAllReports_withNonMatchingFilter_returnsEmptyList()
    throws Exception {
    var calculationId1 = UUID.fromString(
      "1eddb17f-60b6-4b8a-8a9f-d547e6204f27"
    );
    createAndPersistQualityGateReport(
      calculationId1,
      "alpha",
      "api1",
      "v1",
      "1h",
      PASSED
    );

    mockMvc
      .perform(
        get(PATH_LIST_QUALITY_GATE_REPORTS).queryParam(
          "serviceName",
          "nonexistent"
        )
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "0"))
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void findAllReports_withFilter_respectsPagination() throws Exception {
    var calculationId1 = UUID.fromString(
      "bc4600b8-abb4-4775-b650-b2139ffacc35"
    );
    createAndPersistQualityGateReport(
      calculationId1,
      "alpha",
      "api1",
      "v1",
      "1h",
      PASSED
    );
    var calculationId2 = UUID.fromString(
      "5aa1c1ea-bd4d-4d9c-a401-d29e6da06d05"
    );
    createAndPersistQualityGateReport(
      calculationId2,
      "alpha",
      "api2",
      "v1",
      "1h",
      PASSED
    );
    var calculationId3 = UUID.fromString(
      "36c06802-7be1-4ad4-800d-3e7595945e28"
    );
    createAndPersistQualityGateReport(
      calculationId3,
      "alpha",
      "api3",
      "v1",
      "1h",
      PASSED
    );

    mockMvc
      .perform(
        get(PATH_LIST_QUALITY_GATE_REPORTS)
          .queryParam("serviceName", "alpha")
          .queryParam("size", "2")
          .queryParam("page", "0")
      )
      .andExpect(status().isOk())
      .andExpect(header().string(HEADER_X_TOTAL_COUNT, "3"))
      .andExpect(jsonPath("$.length()").value(2));
  }

  private void createSimpleApiTestSet(QualityGateReport qualityGateReport) {
    apiTestRepository.save(
      ApiTest.builder()
        .serviceName("serviceName")
        .apiName("apiName")
        .qualityGateReport(qualityGateReport)
        .apiType(UNSPECIFIED.getVal())
        .build()
    );
  }
}

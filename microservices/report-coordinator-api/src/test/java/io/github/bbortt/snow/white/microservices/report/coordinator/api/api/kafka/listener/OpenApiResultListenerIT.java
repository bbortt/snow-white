/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka.listener;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.CoverageImpliedByFindings.ratioImpliedBy;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.TestData.defaultApiInformation;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.TestData.defaultApiTest;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FAILED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.FINISHED_EXCEPTIONALLY;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.IN_PROGRESS;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.SET;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.awaitility.Awaitility.await;
import static org.springframework.util.ObjectUtils.isEmpty;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.qualitygateapi.dto.QualityGateConfig;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.QualityGateReport;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportParameter;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

class OpenApiResultListenerIT extends AbstractReportCoordinationServiceIT {

  @Autowired
  private KafkaTemplate<
    @NonNull String,
    @NonNull OpenApiCoverageResponseEvent
  > kafkaTemplate;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private ApiTestRepository apiTestRepository;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  @BeforeEach
  void beforeEachSetup() {
    qualityGateApi.resetMappings();
  }

  @Test
  void kafkaEvent_withCoveredCriteria_shouldBePersisted() {
    var calculationId = UUID.fromString("6fa77498-a7aa-48d2-8f1d-dee93eb45780");
    var qualityGateReport = persistInitialQualityGateReport(calculationId);

    sendAndVerifyOpenApiCoverageResponseEventWithOpenApiTestResult(
      qualityGateReport.getQualityGateConfigName(),
      PATH_COVERAGE,
      calculationId,
      ONE,
      PASSED
    );
  }

  @Test
  void kafkaEvent_withUncoveredCriteria_shouldBePersisted() {
    var calculationId = UUID.fromString("dc296f73-8124-4bd3-bc09-5518bdb5be6e");
    var qualityGateReport = persistInitialQualityGateReport(calculationId);

    sendAndVerifyOpenApiCoverageResponseEventWithOpenApiTestResult(
      qualityGateReport.getQualityGateConfigName(),
      HTTP_METHOD_COVERAGE,
      calculationId,
      ZERO,
      FAILED
    );
  }

  @Test
  void kafkaEvent_withException_shouldBePersisted() {
    var calculationId = UUID.fromString("0946c831-cc38-4707-b24c-46fedc7665af");
    var qualityGateConfigName = persistInitialQualityGateReport(calculationId);

    kafkaTemplate.send(
      reportCoordinationServiceProperties
        .getOpenapiCalculationResponse()
        .getTopic(),
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        defaultApiInformation(),
        "Exception that should be persisted"
      )
    );

    await()
      .atMost(1, MINUTES)
      .untilAsserted(
        () -> qualityGateReportRepository.findById(calculationId),
        qualityGateReport ->
          assertThat(qualityGateReport)
            .isPresent()
            .get()
            .satisfies(
              report ->
                assertThat(report.getReportStatus()).isEqualTo(
                  FINISHED_EXCEPTIONALLY
                ),
              report ->
                assertThat(report.getApiTests())
                  .hasSize(1)
                  .first()
                  .satisfies(apiTest ->
                    assertThat(apiTest.getApiTestResults()).isEmpty()
                  )
            )
      );

    qualityGateApi.verifyThat(
      0,
      getRequestedFor(
        urlMatching("/api/rest/v1/quality-gates/" + qualityGateConfigName)
      )
    );
  }

  @Test
  void multipleKafkaEvents_withSameCalculationId_shouldBeAggregated() {
    var calculationId = UUID.fromString("521f9236-369d-4cab-813a-98aa6e46b0a2");

    var qualityGateReport = persistInitialQualityGateReport(calculationId);
    var apiTest = apiTestRepository.save(
      ApiTest.builder()
        .serviceName("serviceName")
        .apiName("otherApiName")
        .apiVersion("apiVersion")
        .apiType(OPENAPI.getVal())
        .build()
        .withQualityGateReport(qualityGateReport)
    );

    sendAndVerifyOpenApiCoverageResponseEventWithOpenApiTestResult(
      qualityGateReport.getQualityGateConfigName(),
      ERROR_RESPONSE_CODE_COVERAGE,
      calculationId,
      ONE,
      IN_PROGRESS
    );

    kafkaTemplate.send(
      reportCoordinationServiceProperties
        .getOpenapiCalculationResponse()
        .getTopic(),
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        ApiInformation.builder()
          .serviceName(apiTest.getServiceName())
          .apiName(apiTest.getApiName())
          .apiVersion(apiTest.getApiVersion())
          .apiType(OPENAPI)
          .build(),
        "Exception that should be persisted"
      )
    );

    await()
      .atMost(1, MINUTES)
      .untilAsserted(
        () -> qualityGateReportRepository.findById(calculationId),
        persistedQualityGateReport ->
          assertThat(persistedQualityGateReport)
            .isPresent()
            .get()
            .satisfies(
              report ->
                assertThat(report.getReportStatus()).isEqualTo(
                  FINISHED_EXCEPTIONALLY
                ),
              report ->
                assertThat(report.getApiTests())
                  .hasSize(2)
                  .satisfiesOnlyOnce(persistedApiTest ->
                    assertThat(persistedApiTest.getApiTestResults()).isEmpty()
                  )
            )
      );
  }

  @Test
  void kafkaEvent_redeliveredForSameApiTest_shouldReplaceOverlappingCriterionAndKeepOthers() {
    var calculationId = UUID.fromString("3e6f9f2a-1b1a-4b4a-8c2b-7f6e5d4c3b2a");
    var qualityGateReport = persistInitialQualityGateReport(calculationId);

    createQualityGateApiWiremockStub(
      qualityGateReport.getQualityGateConfigName(),
      PATH_COVERAGE,
      HTTP_METHOD_COVERAGE,
      ERROR_RESPONSE_CODE_COVERAGE
    );

    var topic = reportCoordinationServiceProperties
      .getOpenapiCalculationResponse()
      .getTopic();

    var firstDuration = Duration.ofMillis(1000);
    kafkaTemplate.send(
      topic,
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        defaultApiInformation(),
        Set.of(
          new OpenApiTestResult(PATH_COVERAGE, ONE, firstDuration),
          new OpenApiTestResult(HTTP_METHOD_COVERAGE, ONE, firstDuration)
        )
      )
    );

    await()
      .atMost(1, MINUTES)
      .untilAsserted(
        () -> qualityGateReportRepository.findById(calculationId),
        persistedQualityGateReport ->
          assertThat(apiTestResultsOf(persistedQualityGateReport)).hasSize(2)
      );

    var secondDuration = Duration.ofMillis(2000);
    kafkaTemplate.send(
      topic,
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        defaultApiInformation(),
        Set.of(
          new OpenApiTestResult(PATH_COVERAGE, ZERO, secondDuration),
          new OpenApiTestResult(
            ERROR_RESPONSE_CODE_COVERAGE,
            ONE,
            secondDuration
          )
        )
      )
    );

    await()
      .atMost(1, MINUTES)
      .untilAsserted(
        () -> qualityGateReportRepository.findById(calculationId),
        persistedQualityGateReport -> {
          assertThat(persistedQualityGateReport)
            .isPresent()
            .get()
            .extracting(QualityGateReport::getReportStatus)
            .isEqualTo(FAILED);

          var resultsByCriteria = apiTestResultsOf(persistedQualityGateReport);
          assertThat(resultsByCriteria).hasSize(3);

          assertThat(resultsByCriteria.get(PATH_COVERAGE.name()))
            .extracting(ApiTestResult::getCoverage, ApiTestResult::getDuration)
            .containsExactly(ZERO.setScale(2, HALF_UP), secondDuration);
          assertThat(resultsByCriteria.get(HTTP_METHOD_COVERAGE.name()))
            .extracting(ApiTestResult::getCoverage, ApiTestResult::getDuration)
            .containsExactly(ONE.setScale(2, HALF_UP), firstDuration);
          assertThat(resultsByCriteria.get(ERROR_RESPONSE_CODE_COVERAGE.name()))
            .extracting(ApiTestResult::getCoverage, ApiTestResult::getDuration)
            .containsExactly(ONE.setScale(2, HALF_UP), secondDuration);
        }
      );
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
  @VerifiesSw(
    SwTraceables.SW_020_REDELIVERED_CRITERION_RESULT_REPLACES_EXISTING_ONE
  )
  @VerifiesCon(ConTraceables.CON_009_COVERAGE_AGREES_WITH_FINDINGS)
  void kafkaEvent_redeliveredWithFindings_shouldReplaceThemWholesale() {
    var calculationId = UUID.fromString("7b5c4d3e-2f1a-4b8c-9d0e-1f2a3b4c5d6e");
    var qualityGateReport = persistInitialQualityGateReport(calculationId);

    createQualityGateApiWiremockStub(
      qualityGateReport.getQualityGateConfigName(),
      PATH_COVERAGE,
      HTTP_METHOD_COVERAGE,
      ERROR_RESPONSE_CODE_COVERAGE
    );

    var topic = reportCoordinationServiceProperties
      .getOpenapiCalculationResponse()
      .getTopic();

    kafkaTemplate.send(
      topic,
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        defaultApiInformation(),
        Set.of(
          new OpenApiTestResult(
            PATH_COVERAGE,
            new BigDecimal("0.50"),
            Duration.ofMillis(1000),
            null,
            List.of(
              finding("/paths/~1kept", COVERED, "aaaa1"),
              finding("/paths/~1superseded", UNCOVERED)
            )
          ),
          new OpenApiTestResult(
            HTTP_METHOD_COVERAGE,
            ONE.setScale(2, HALF_UP),
            Duration.ofMillis(1000),
            null,
            List.of(finding("/paths/~1untouched/get", COVERED, "aaaa2"))
          )
        )
      )
    );

    await()
      .atMost(1, MINUTES)
      .untilAsserted(
        () -> persistedFindings(calculationId),
        findings -> assertThat(findings).hasSize(3)
      );
    assertCoverageAgreesWithItsPersistedFindings(calculationId);

    kafkaTemplate.send(
      topic,
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        defaultApiInformation(),
        Set.of(
          new OpenApiTestResult(
            PATH_COVERAGE,
            new BigDecimal("0.33"),
            Duration.ofMillis(2000),
            null,
            List.of(
              finding("/paths/~1kept", COVERED, "bbbb1"),
              finding("/paths/~1added", UNCOVERED),
              finding("/paths/~1alsoAdded", UNCOVERED)
            )
          ),
          new OpenApiTestResult(
            ERROR_RESPONSE_CODE_COVERAGE,
            ZERO.setScale(2, HALF_UP),
            Duration.ofMillis(2000),
            null,
            List.of(finding("/paths/~1kept/get/responses/500", UNCOVERED))
          )
        )
      )
    );

    await()
      .atMost(1, MINUTES)
      .untilAsserted(
        () -> persistedFindings(calculationId),
        findings ->
          assertThat(findings)
            .extracting(
              row -> row.get("api_test_criteria"),
              row -> row.get("spec_pointer")
            )
            .containsExactlyInAnyOrder(
              // The superseded delivery's own target is gone rather than sitting beside the new
              // ones, and the target both deliveries judged is present exactly once.
              tuple(PATH_COVERAGE.name(), "/paths/~1kept"),
              tuple(PATH_COVERAGE.name(), "/paths/~1added"),
              tuple(PATH_COVERAGE.name(), "/paths/~1alsoAdded"),
              tuple(HTTP_METHOD_COVERAGE.name(), "/paths/~1untouched/get"),
              tuple(
                ERROR_RESPONSE_CODE_COVERAGE.name(),
                "/paths/~1kept/get/responses/500"
              )
            )
      );

    assertThat(persistedEvidence(calculationId))
      .extracting(row -> row.get("spec_pointer"), row -> row.get("trace_id"))
      .containsExactlyInAnyOrder(
        // The replaced finding's evidence went with it: the trace the superseded delivery matched
        // on is not what the drilldown would show.
        tuple("/paths/~1kept", "bbbb1"),
        tuple("/paths/~1untouched/get", "aaaa2")
      );

    assertCoverageAgreesWithItsPersistedFindings(calculationId);
  }

  private static ApiTestFinding finding(
    String specPointer,
    FindingStatus status,
    String... traceIds
  ) {
    return ApiTestFinding.builder()
      .specPointer(specPointer)
      .status(status)
      .httpPath("/api/v1/users")
      .httpMethod("GET")
      .evidence(
        stream(traceIds)
          .map(traceId -> new FindingEvidence(traceId, null))
          .toList()
      )
      .build();
  }

  private List<Map<String, Object>> persistedFindings(UUID calculationId) {
    return jdbcTemplate.queryForList(
      """
      SELECT f.api_test_criteria, f.spec_pointer, f.status
        FROM api_test_finding f
        JOIN api_test t ON t.id = f.api_test
       WHERE t.calculation_id = ?
      """,
      calculationId
    );
  }

  private List<Map<String, Object>> persistedEvidence(UUID calculationId) {
    return jdbcTemplate.queryForList(
      """
      SELECT f.spec_pointer, e.trace_id, e.test_case_name
        FROM finding_evidence e
        JOIN api_test_finding f ON f.id = e.api_test_finding
        JOIN api_test t ON t.id = f.api_test
       WHERE t.calculation_id = ?
      """,
      calculationId
    );
  }

  /**
   * The stored ratio recomputed from the rows that are supposed to explain it. A result carrying no
   * findings is exempt rather than in violation: a report written before the migration keeps the
   * ratio it was calculated with and has no evidence to disagree with.
   */
  private void assertCoverageAgreesWithItsPersistedFindings(
    UUID calculationId
  ) {
    var findingsByCriteria = persistedFindings(calculationId)
      .stream()
      .collect(groupingBy(row -> row.get("api_test_criteria")));

    var results = jdbcTemplate.queryForList(
      """
      SELECT r.api_test_criteria, r.coverage
        FROM api_test_result r
        JOIN api_test t ON t.id = r.api_test
       WHERE t.calculation_id = ?
      """,
      calculationId
    );

    assertThat(results)
      .isNotEmpty()
      .allSatisfy(result -> {
        var findings = findingsByCriteria.get(result.get("api_test_criteria"));

        if (isEmpty(findings)) {
          return;
        }

        assertThat((BigDecimal) result.get("coverage"))
          .as("coverage of %s", result.get("api_test_criteria"))
          .isEqualByComparingTo(ratioImpliedBy(findings));
      });
  }

  private Map<String, ApiTestResult> apiTestResultsOf(
    Optional<QualityGateReport> persistedQualityGateReport
  ) {
    return persistedQualityGateReport
      .orElseThrow()
      .getApiTests()
      .iterator()
      .next()
      .getApiTestResults()
      .stream()
      .collect(toMap(ApiTestResult::getApiTestCriteria, identity()));
  }

  private @NonNull QualityGateReport persistInitialQualityGateReport(
    UUID calculationId
  ) {
    var qualityGateConfigName = "minimal";
    var qualityGateReport = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(calculationId)
        .qualityGateConfigName(qualityGateConfigName)
        .reportParameter(
          ReportParameter.builder()
            .calculationId(calculationId)
            .lookbackWindow("1h")
            .build()
        )
        .build()
    );

    apiTestRepository.save(
      defaultApiTest().withQualityGateReport(qualityGateReport)
    );

    return qualityGateReport;
  }

  private void sendAndVerifyOpenApiCoverageResponseEventWithOpenApiTestResult(
    String qualityGateConfigName,
    OpenApiCoverageCriteria openApiCriterion,
    UUID calculationId,
    BigDecimal one,
    ReportStatus reportStatus
  ) {
    var qualityGateByNameEndpoint = createQualityGateApiWiremockStub(
      qualityGateConfigName,
      openApiCriterion
    );

    var duration = Duration.ofMillis(1234);
    kafkaTemplate.send(
      reportCoordinationServiceProperties
        .getOpenapiCalculationResponse()
        .getTopic(),
      calculationId.toString(),
      new OpenApiCoverageResponseEvent(
        defaultApiInformation(),
        Set.of(new OpenApiTestResult(openApiCriterion, one, duration))
      )
    );

    assertThatEntityHasBeenUpdated(
      calculationId,
      openApiCriterion,
      reportStatus,
      one.setScale(2, HALF_UP),
      duration
    );

    qualityGateApi.verifyThat(
      getRequestedFor(urlEqualTo(qualityGateByNameEndpoint))
    );
  }

  private @NonNull String createQualityGateApiWiremockStub(
    String qualityGateConfigName,
    OpenApiCoverageCriteria... openApiCriteria
  ) {
    var qualityGateConfig = new QualityGateConfig().name(qualityGateConfigName);
    for (var openApiCriterion : openApiCriteria) {
      qualityGateConfig.addOpenApiCoverageCriteriaItem(openApiCriterion.name());
    }

    var qualityGateByNameEndpoint =
      "/api/rest/v1/quality-gates/" + qualityGateConfigName;
    qualityGateApi.register(
      get(qualityGateByNameEndpoint).willReturn(
        okJson(jsonMapper.writeValueAsString(qualityGateConfig))
      )
    );

    return qualityGateByNameEndpoint;
  }

  private void assertThatEntityHasBeenUpdated(
    UUID calculationId,
    OpenApiCoverageCriteria openApiCriterion,
    ReportStatus reportStatus,
    BigDecimal coverage,
    Duration duration
  ) {
    await()
      .atMost(1, MINUTES)
      .untilAsserted(
        () -> qualityGateReportRepository.findById(calculationId),
        qualityGateReport ->
          assertThat(qualityGateReport)
            .isPresent()
            .get()
            .satisfies(
              report ->
                assertThat(report.getReportStatus()).isEqualTo(reportStatus),
              report ->
                assertThat(report.getApiTests())
                  .hasSizeBetween(1, 2)
                  .first()
                  .extracting(ApiTest::getApiTestResults)
                  .asInstanceOf(SET)
                  .hasSize(1)
                  .first()
                  .asInstanceOf(type(ApiTestResult.class))
                  .satisfies(
                    openApiResult ->
                      assertThat(openApiResult.getApiTestCriteria())
                        .isNotNull()
                        .isEqualTo(openApiCriterion.name()),
                    openApiResult ->
                      assertThat(openApiResult.getCoverage()).isEqualTo(
                        coverage
                      ),
                    openApiResult ->
                      assertThat(openApiResult.getIncludedInReport()).isTrue(),
                    openApiResult ->
                      assertThat(openApiResult.getDuration()).isEqualTo(
                        duration
                      ),
                    openApiResult ->
                      assertThat(
                        openApiResult.getAdditionalInformation()
                      ).isNull()
                  )
            )
      );
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.UNSPECIFIED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.ERROR_RESPONSE_CODE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ReportStatus.PASSED;
import static java.lang.Boolean.TRUE;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.AbstractReportCoordinationServiceIT;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.ApiTestRepository;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.repository.QualityGateReportRepository;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Asserted through JDBC rather than through the entity graph: both collections are lazy on purpose,
 * and a test that navigates them inside a transaction would not notice a column the mapping never
 * writes.
 */
class ApiTestFindingIT extends AbstractReportCoordinationServiceIT {

  private static final UUID CALCULATION_ID = UUID.fromString(
    "9c1d2e3f-4a5b-4c6d-8e7f-0a1b2c3d4e5f"
  );

  @Autowired
  private ApiTestRepository apiTestRepository;

  @Autowired
  private QualityGateReportRepository qualityGateReportRepository;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @AfterEach
  void afterEachTeardown() {
    qualityGateReportRepository.deleteById(CALCULATION_ID);
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
  void shouldPersistEveryFindingBesideTheResultItExplains() {
    persistApiTestResultWithFindings();

    assertThat(
      jdbcTemplate.queryForList(
        """
        SELECT f.spec_pointer, f.status, f.http_path, f.http_method, f.response_code,
               f.parameter_name, f.content_type, f.api_test_criteria
          FROM api_test_finding f
        """
      )
    )
      .extracting(
        row -> row.get("spec_pointer"),
        row -> row.get("status"),
        row -> row.get("api_test_criteria")
      )
      .containsExactlyInAnyOrder(
        tuple(
          "/paths/~1api~1v1~1users/get/responses/404",
          COVERED.getVal(),
          ERROR_RESPONSE_CODE_COVERAGE.name()
        ),
        tuple(
          "/paths/~1api~1v1~1users/get/responses/500",
          UNCOVERED.getVal(),
          ERROR_RESPONSE_CODE_COVERAGE.name()
        ),
        tuple(
          "/paths/~1api~1v1~1users/delete",
          NOT_APPLICABLE.getVal(),
          ERROR_RESPONSE_CODE_COVERAGE.name()
        )
      );

    assertThat(
      jdbcTemplate.queryForList(
        """
        SELECT f.spec_pointer, f.http_path, f.http_method, f.response_code, f.parameter_name,
               f.content_type
          FROM api_test_finding f
         WHERE f.spec_pointer = '/paths/~1api~1v1~1users/get/responses/404'
        """
      )
    )
      .singleElement()
      .satisfies(
        row -> assertThat(row.get("http_path")).isEqualTo("/api/v1/users"),
        row -> assertThat(row.get("http_method")).isEqualTo("GET"),
        row -> assertThat(row.get("response_code")).isEqualTo("404"),
        row -> assertThat(row.get("parameter_name")).isNull(),
        row -> assertThat(row.get("content_type")).isEqualTo("application/json")
      );
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
  void shouldPersistEvidenceWithANullTestCaseNameUntilAHarnessEmitsOne() {
    persistApiTestResultWithFindings();

    assertThat(
      jdbcTemplate.queryForList(
        """
        SELECT e.trace_id, e.test_case_name
          FROM finding_evidence e
        """
      )
    )
      .extracting(row -> row.get("trace_id"), row -> row.get("test_case_name"))
      .containsExactlyInAnyOrder(
        tuple("1f8b0c4d2e3a4b5c6d7e8f9a0b1c2d3e", null),
        tuple("2e3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c", "shouldReturnNotFound"),
        tuple("3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b", null)
      );
  }

  @Test
  @VerifiesArch(ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE)
  void shouldLeaveNoFindingBehindWhenTheReportIsDeleted() {
    persistApiTestResultWithFindings();

    qualityGateReportRepository.deleteById(CALCULATION_ID);

    assertThat(
      jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM api_test_finding",
        Long.class
      )
    ).isZero();
    assertThat(
      jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM finding_evidence",
        Long.class
      )
    ).isZero();
  }

  private void persistApiTestResultWithFindings() {
    var qualityGateReport = qualityGateReportRepository.save(
      QualityGateReport.builder()
        .calculationId(CALCULATION_ID)
        .qualityGateConfigName("qualityGateConfigName")
        .reportParameter(
          ReportParameter.builder().calculationId(CALCULATION_ID).build()
        )
        .reportStatus(PASSED.getVal())
        .build()
    );

    var apiTest = apiTestRepository.save(
      ApiTest.builder()
        .serviceName("serviceName")
        .apiName("apiName")
        .apiVersion("apiVersion")
        .apiType(UNSPECIFIED.getVal())
        .qualityGateReport(qualityGateReport)
        .build()
    );

    var apiTestResult = ApiTestResult.builder()
      .apiTestCriteria(ERROR_RESPONSE_CODE_COVERAGE.name())
      .coverage(ZERO)
      .includedInReport(TRUE)
      .duration(Duration.ofSeconds(1))
      .apiTest(apiTest)
      .build();

    apiTestResult
      .getFindings()
      .addAll(
        Set.of(
          finding(
            apiTestResult,
            "/paths/~1api~1v1~1users/get/responses/404",
            COVERED,
            "404",
            List.of(
              evidence("1f8b0c4d2e3a4b5c6d7e8f9a0b1c2d3e", null),
              evidence(
                "2e3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c",
                "shouldReturnNotFound"
              )
            )
          ),
          finding(
            apiTestResult,
            "/paths/~1api~1v1~1users/get/responses/500",
            UNCOVERED,
            "500",
            List.of()
          ),
          finding(
            apiTestResult,
            "/paths/~1api~1v1~1users/delete",
            NOT_APPLICABLE,
            null,
            List.of(evidence("3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b", null))
          )
        )
      );

    apiTestRepository.save(apiTest.withApiTestResults(Set.of(apiTestResult)));
  }

  private static FindingEvidence evidence(
    String traceId,
    @Nullable String testCaseName
  ) {
    return FindingEvidence.builder()
      .traceId(traceId)
      .testCaseName(testCaseName)
      .build();
  }

  private static ApiTestFinding finding(
    ApiTestResult apiTestResult,
    String specPointer,
    FindingStatus status,
    String responseCode,
    List<FindingEvidence> evidence
  ) {
    var finding = ApiTestFinding.builder()
      .specPointer(specPointer)
      .status(status.getVal())
      .httpPath("/api/v1/users")
      .httpMethod(NOT_APPLICABLE.equals(status) ? "DELETE" : "GET")
      .responseCode(responseCode)
      .contentType("application/json")
      .apiTestResult(apiTestResult)
      .build();

    finding.getEvidence().addAll(evidence);

    return finding;
  }
}

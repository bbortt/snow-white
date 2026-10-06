/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.mapper;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PATH_COVERAGE;
import static java.math.BigDecimal.ONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.FindingStatus;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTestResult;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiTestResultMapperUnitTest {

  private ApiTestResultMapper fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiTestResultMapperImpl();
  }

  @Nested
  class FromDtosTest {

    @Mock
    private ApiTest apiTestMock;

    @Test
    void shouldLinkApiTestToEachResult() {
      Set<ApiTestResult> apiTestResults = fixture.fromDtos(
        Set.of(
          new OpenApiTestResult(PATH_COVERAGE, ONE, Duration.ofSeconds(1))
        ),
        apiTestMock
      );

      assertThat(apiTestResults)
        .isNotNull()
        .isNotEmpty()
        .allSatisfy(apiTestResult ->
          assertThat(apiTestResult.getApiTest()).isEqualTo(apiTestMock)
        );
    }

    @Test
    void shouldLeaveFindingsEmpty_whenTheResultCarriesNone() {
      Set<ApiTestResult> apiTestResults = fixture.fromDtos(
        Set.of(
          new OpenApiTestResult(PATH_COVERAGE, ONE, Duration.ofSeconds(1))
        ),
        apiTestMock
      );

      assertThat(apiTestResults)
        .singleElement()
        .satisfies(apiTestResult ->
          assertThat(apiTestResult.getFindings()).isEmpty()
        );
    }
  }

  @Nested
  class FindingsTest {

    @Mock
    private ApiTest apiTestMock;

    @Test
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    void shouldMapEveryFindingWithItsEvidenceAndBackReference() {
      var result = fixture.fromDto(
        new OpenApiTestResult(
          PATH_COVERAGE,
          ONE,
          Duration.ofSeconds(1),
          null,
          List.of(
            ApiTestFinding.builder()
              .specPointer("/paths/~1api~1v1~1users/get/responses/404")
              .status(COVERED)
              .httpPath("/api/v1/users")
              .httpMethod("GET")
              .responseCode("404")
              .contentType("application/json")
              .evidence(
                List.of(
                  new FindingEvidence("traceId", null),
                  new FindingEvidence("otherTraceId", "testCaseName")
                )
              )
              .build()
          )
        ),
        apiTestMock
      );

      assertThat(result.getFindings())
        .singleElement()
        .satisfies(
          finding ->
            assertThat(finding.getSpecPointer()).isEqualTo(
              "/paths/~1api~1v1~1users/get/responses/404"
            ),
          finding ->
            assertThat(finding.getStatus().name()).isEqualTo("COVERED"),
          finding ->
            assertThat(finding.getHttpPath()).isEqualTo("/api/v1/users"),
          finding -> assertThat(finding.getHttpMethod()).isEqualTo("GET"),
          finding -> assertThat(finding.getResponseCode()).isEqualTo("404"),
          finding -> assertThat(finding.getParameterName()).isNull(),
          finding ->
            assertThat(finding.getContentType()).isEqualTo("application/json"),
          // The back-reference is what writes the foreign key: a finding without it
          // cannot be persisted at all.
          finding -> assertThat(finding.getApiTestResult()).isSameAs(result),
          finding ->
            assertThat(finding.getEvidence())
              .extracting(
                evidence -> evidence.getTraceId(),
                evidence -> evidence.getTestCaseName()
              )
              .containsExactlyInAnyOrder(
                tuple("traceId", null),
                tuple("otherTraceId", "testCaseName")
              )
        );
    }

    @EnumSource
    @ParameterizedTest
    @VerifiesArch(
      ArchTraceables.ARCH_012_FINDINGS_ON_THE_EVENT_COVERAGE_AS_CACHE
    )
    void shouldTranslateEveryStatusToItsOwnConstant(FindingStatus status) {
      var result = fixture.fromDto(
        new OpenApiTestResult(
          PATH_COVERAGE,
          ONE,
          Duration.ofSeconds(1),
          null,
          List.of(
            ApiTestFinding.builder()
              .specPointer("/paths/~1api")
              .status(status)
              .evidence(List.of())
              .build()
          )
        ),
        apiTestMock
      );

      assertThat(result.getFindings())
        .singleElement()
        .satisfies(finding ->
          assertThat(finding.getStatus().name()).isEqualTo(status.name())
        );
    }
  }
}

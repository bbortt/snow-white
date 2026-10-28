/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.tool;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.mcp.api.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.apiindexapi.dto.GetAllApis200ResponseInner;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.criteriaapi.api.CriteriaApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.criteriaapi.dto.OpenApiCriterion;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.api.ReportApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.dto.GetReportByCalculationId200Response;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.dto.ListQualityGateReports200ResponseInner;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Each tool hands back what its client answered, unchanged. {@code SnowWhiteToolsRestAgreementIT}
 * asserts that value against the REST surface it came from; this asserts the delegation itself,
 * including the argument padding a generated signature forces and the two absences that are
 * reported rather than answered with nothing.
 */
@ExtendWith({ MockitoExtension.class })
class SnowWhiteToolsUnitTest {

  private static final UUID CALCULATION_ID = UUID.fromString(
    "3f1a6b0e-8c2d-4f5a-9b7e-1d0c2a4e6f80"
  );

  @Mock
  private ApiIndexApi apiIndexApiMock;

  @Mock
  private CriteriaApi criteriaApiMock;

  @Mock
  private ReportApi reportApiMock;

  @InjectMocks
  private SnowWhiteTools fixture;

  @Nested
  class ListApisTest {

    @Test
    void answersWithTheIndexedApisTheClientServes() {
      var indexedApis = List.of(new GetAllApis200ResponseInner());
      doReturn(indexedApis)
        .when(apiIndexApiMock)
        .getAllApis(null, null, null, "payments-service", "payments-api");

      assertThat(
        fixture.listApis("payments-service", "payments-api")
      ).isEqualTo(indexedApis);
    }

    @Test
    void passesNoFilterThroughAsNull() {
      doReturn(emptyList())
        .when(apiIndexApiMock)
        .getAllApis(null, null, null, null, null);

      assertThat(fixture.listApis(null, null)).isEmpty();
      verify(apiIndexApiMock).getAllApis(null, null, null, null, null);
    }
  }

  @Nested
  class ExplainCriterionTest {

    /** The wanted criterion is second, so answering with the first is not an answer. */
    private static final List<OpenApiCriterion> CRITERIA = List.of(
      new OpenApiCriterion().id("PATH_COVERAGE"),
      new OpenApiCriterion().id("ERROR_CODE_COVERAGE")
    );

    @Test
    void answersWithTheNamedCriterion() {
      doReturn(CRITERIA).when(criteriaApiMock).listOpenApiCoverageCriteria();

      assertThat(fixture.explainCriterion("ERROR_CODE_COVERAGE")).isSameAs(
        CRITERIA.get(1)
      );
    }

    @Test
    void reportsAnIdTheCriteriaReadDoesNotServe() {
      doReturn(CRITERIA).when(criteriaApiMock).listOpenApiCoverageCriteria();

      assertThatThrownBy(() -> fixture.explainCriterion("NO_SUCH_CRITERION"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "No OpenAPI coverage criterion is named 'NO_SUCH_CRITERION'."
        );
    }
  }

  @Nested
  class GetCoverageTest {

    @Test
    void answersWithTheOneReportTheListingServes() {
      var report = new ListQualityGateReports200ResponseInner();
      doReturn(List.of(report))
        .when(reportApiMock)
        .listQualityGateReports(
          null,
          1,
          null,
          "payments-service",
          "payments-api",
          "1.4.0"
        );

      assertThat(
        fixture.getCoverage("payments-service", "payments-api", "1.4.0")
      ).isSameAs(report);
    }

    @Test
    void reportsThatNoReportMatches() {
      doReturn(emptyList())
        .when(reportApiMock)
        .listQualityGateReports(null, 1, null, "unknown-service", null, null);

      assertThatThrownBy(() ->
        fixture.getCoverage("unknown-service", null, null)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "No quality-gate report matches service 'unknown-service', API 'null', version 'null'."
        );
    }
  }

  @Nested
  class ListFindingsTest {

    @Test
    void answersWithTheReportTheClientServes() {
      var report = new GetReportByCalculationId200Response();
      doReturn(report)
        .when(reportApiMock)
        .getReportByCalculationId(CALCULATION_ID);

      assertThat(fixture.listFindings(CALCULATION_ID)).isSameAs(report);
    }
  }
}

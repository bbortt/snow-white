/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.tool;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SysTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSys;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;
import org.wiremock.spring.EnableWireMock;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * A value an MCP tool answers with is structurally equal to the same value read through
 * the REST endpoint it came from.
 *
 * <p>The fixtures below are written out rather than built from the generated DTOs on
 * purpose. A field the client drops on the way through would then be missing on the MCP
 * side and present on the REST side, which is the "reshaping may drop nothing" half of the
 * façade decision — including the judged and waived counts a waiver-emptied criterion is
 * read by, and the {@code isIncludedInQualityGate} flag.
 */
@VerifiesSys(SysTraceables.SYS_015_AGENT_QUERYABLE_CONSUMPTION_PATH)
@VerifiesArch(ArchTraceables.ARCH_021_MCP_SURFACE_IS_A_FACADE)
@EnableWireMock
@SpringBootTest(
  webEnvironment = RANDOM_PORT,
  properties = {
    "snow.white.mcp.api.api-index.base-url=${wiremock.server.baseUrl}",
    "snow.white.mcp.api.criteria.base-url=${wiremock.server.baseUrl}",
    "snow.white.mcp.api.report.base-url=${wiremock.server.baseUrl}",
    "management.server.port=0",
  }
)
class SnowWhiteToolsRestAgreementIT {

  private static final UUID CALCULATION_ID = UUID.fromString(
    "3f1a6b0e-8c2d-4f5a-9b7e-1d0c2a4e6f80"
  );

  private static final String APIS_PATH = "/api/rest/v1/apis";
  private static final String CRITERIA_PATH = "/api/rest/v1/criteria/openapi";
  private static final String REPORTS_PATH = "/api/rest/v1/reports";
  private static final String REPORT_PATH = REPORTS_PATH + "/" + CALCULATION_ID;

  private static final String INDEXED_APIS = """
  [{"serviceName":"payments-service","apiName":"payments-api","apiVersion":"1.4.0","sourceUrl":"https://example.test/payments.yaml","apiType":"OPENAPI","prerelease":false,"content":"openapi: 3.1.0"}]""";

  private static final String CRITERIA = """
  [{"id":"PATH_COVERAGE","name":"Path Coverage","description":"Share of documented paths exercised by a trace."},
   {"id":"ERROR_CODE_COVERAGE","name":"Error Code Coverage","description":"Share of documented error responses observed."}]""";

  private static final String CALCULATION_REQUEST = """
  {"includeApis":[{"serviceName":"payments-service","apiName":"payments-api","apiVersion":"1.4.0"}],"lookbackWindow":"24h","attributeFilters":{"deployment.environment":"staging"},"waivers":[{"serviceName":"payments-service","apiName":"payments-api","apiVersion":"1.4.0","path":"/payments/{id}","method":"DELETE","responseCode":"410","parameterName":null,"contentType":null,"criterion":"ERROR_CODE_COVERAGE","reason":"endpoint retired","expires":"2027-01-01"}]}""";

  private static final String FINDINGS = """
  [{"status":"UNCOVERED","specPointer":"/paths/~1payments~1{id}/get/responses/404","httpPath":"/payments/{id}","httpMethod":"GET","responseCode":"404","parameterName":null,"contentType":"application/json","evidence":[{"traceId":"4bf92f3577b34da6a3ce929d0e0e4736","testCaseName":"GET /payments/{id} returns 404"}],"waiverReason":null,"waiverExpiresOn":null}]""";

  /**
   * The listing and the single report are the same value; only the listing's summarised
   * test results leave out the findings the full read carries.
   */
  private static final String REPORT_BODY = """
  {"calculationId":"%s","qualityGateConfigName":"default","minCoveragePercentage":80,"status":"FAILED","calculationRequest":%s,"interfaces":[{"serviceName":"payments-service","apiName":"payments-api","apiVersion":"1.4.0","apiType":"OPENAPI","status":"FAILED","stackTrace":null,"testResults":[{"id":"PATH_COVERAGE","coverage":0.75,"judgedTargetCount":4,"waivedTargetCount":1,"additionalInformation":null,"isIncludedInQualityGate":true%%s},{"id":"ERROR_CODE_COVERAGE","coverage":0.0,"judgedTargetCount":0,"waivedTargetCount":3,"additionalInformation":"emptied by waivers","isIncludedInQualityGate":false%%s}]}],"initiatedAt":"2026-10-20T08:15:30Z"}""".formatted(
    CALCULATION_ID,
    CALCULATION_REQUEST
  );

  private static final String REPORTS =
    "[" + REPORT_BODY.formatted("", "") + "]";

  private static final String REPORT = REPORT_BODY.formatted(
    ",\"findings\":" + FINDINGS,
    ",\"findings\":[]"
  );

  private static final JsonMapper JSON = JsonMapper.builder().build();

  @Autowired
  private SnowWhiteTools snowWhiteTools;

  @Value("${wiremock.server.baseUrl}")
  private String baseUrl;

  @Test
  void listApisAgreesWithTheApiIndexRead() {
    stubFor(get(urlPathEqualTo(APIS_PATH)).willReturn(okJson(INDEXED_APIS)));

    assertThat(
      asTree(snowWhiteTools.listApis("payments-service", null))
    ).isEqualTo(restRead(APIS_PATH));
  }

  @Test
  void explainCriterionAgreesWithTheElementTheCriteriaReadServes() {
    stubFor(get(urlPathEqualTo(CRITERIA_PATH)).willReturn(okJson(CRITERIA)));

    assertThat(asTree(snowWhiteTools.explainCriterion("ERROR_CODE_COVERAGE")))
      .as("the named element, verbatim")
      .isEqualTo(
        restRead(CRITERIA_PATH)
          .valueStream()
          .filter(criterion ->
            "ERROR_CODE_COVERAGE".equals(criterion.path("id").asString())
          )
          .findFirst()
          .orElseThrow()
      );
  }

  @Test
  void getCoverageAgreesWithTheReportListingRead() {
    stubFor(get(urlPathEqualTo(REPORTS_PATH)).willReturn(okJson(REPORTS)));

    assertThat(
      asTree(
        snowWhiteTools.getCoverage("payments-service", "payments-api", "1.4.0")
      )
    )
      .as(
        "the most recent report the listing serves, including every published count"
      )
      .isEqualTo(restRead(REPORTS_PATH).get(0));
  }

  @Test
  void listFindingsAgreesWithTheReportRead() {
    stubFor(get(urlPathEqualTo(REPORT_PATH)).willReturn(okJson(REPORT)));

    assertThat(asTree(snowWhiteTools.listFindings(CALCULATION_ID))).isEqualTo(
      restRead(REPORT_PATH)
    );
  }

  @Test
  void explainCriterionRefusesAnIdTheCriteriaReadDoesNotServe() {
    stubFor(get(urlPathEqualTo(CRITERIA_PATH)).willReturn(okJson(CRITERIA)));

    assertThatThrownBy(() ->
      snowWhiteTools.explainCriterion("NO_SUCH_CRITERION")
    )
      .as("an absent criterion is reported, not invented")
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("NO_SUCH_CRITERION");
  }

  @Test
  void getCoverageRefusesWhenTheListingReadIsEmpty() {
    stubFor(get(urlPathEqualTo(REPORTS_PATH)).willReturn(okJson("[]")));

    assertThatThrownBy(() ->
      snowWhiteTools.getCoverage("unknown-service", null, null)
    )
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("unknown-service");
  }

  /** The REST surface as a plain consumer sees it — no generated client in between. */
  private JsonNode restRead(String path) {
    return JSON.readTree(
      RestClient.create()
        .get()
        .uri(baseUrl + path)
        .retrieve()
        .body(String.class)
    );
  }

  /**
   * Serialised and re-read rather than converted in memory: a tool result goes to its client
   * as JSON text, and comparing trees built the same way keeps the assertion about values
   * instead of about which {@code NumericNode} subclass a {@code BigDecimal} lands in.
   */
  private static JsonNode asTree(Object toolResult) {
    return JSON.readTree(JSON.writeValueAsString(toolResult));
  }
}

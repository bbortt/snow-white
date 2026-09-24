/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.UNCOVERED;
import static java.math.RoundingMode.HALF_UP;
import static java.util.Locale.ROOT;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.ApiTestFinding;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingEvidence;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class ParameterCoverageCalculatorUnitTest {

  private ParameterCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ParameterCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenParameterCoverage() {
      boolean result = fixture.accepts(PARAMETER_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotParameterCoverage(
      OpenApiCoverageCriteria openApiCriteria
    ) {
      if (PARAMETER_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllQueryParametersCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(
            createParameter("page", "query", false),
            createParameter("size", "query", false)
          )
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "page=1&size=10")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PARAMETER_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn50Percent_whenHalfParametersCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(
            createParameter("page", "query", false),
            createParameter("size", "query", false)
          )
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "page=1")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PARAMETER_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following parameters are uncovered: `GET_/api/v1/users [query: size]`"
          )
      );
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_004_PARAMETER_COVERAGE_MATCHES_BY_TOKEN_NOT_SUBSTRING
    )
    void shouldNotCoverQueryParameter_whenItsNameIsOnlyASubstringOfAnotherToken() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("id", "query", true))
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "validId=5")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following parameters are uncovered: `GET_/api/v1/users [query: id]`"
          )
      );
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_004_PARAMETER_COVERAGE_MATCHES_BY_TOKEN_NOT_SUBSTRING
    )
    void shouldCoverQueryParameter_whenItsOwnTokenIsPresentAlongsideALookalike() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("id", "query", true))
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "id=5&validId=9")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn0Percent_whenNoParametersCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(
            createParameter("page", "query", false),
            createParameter("size", "query", false)
          )
        )
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PARAMETER_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r -> assertThat(r.additionalInformation()).isNotNull()
      );
    }

    @Test
    void shouldReturn100Percent_whenPathParametersCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users/{userId}",
          List.of(createParameter("userId", "path", true))
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users/{userId}", "")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PARAMETER_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn100Percent_whenTelemetryUsesConcretePathForTemplate() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/pung/{message}",
          List.of(createParameter("message", "path", true))
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/pung/hello", "")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldHandleHeaderParameters() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("X-Custom-Header", "header", false))
        )
      );

      var pathToTelemetryMap = createTelemetryWithHeader(
        Map.of("GET_/api/v1/users", "x-custom-header")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn100Percent_whenNoParametersDefined() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of("GET_/api/v1/users", List.of())
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "page=1")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldHandleMixedParameterTypes() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users/{userId}/orders",
          List.of(
            createParameter("userId", "path", true),
            createParameter("status", "query", false),
            createParameter("X-Request-Id", "header", false)
          )
        )
      );

      var telemetryData = createTelemetryDataWithMultipleParams(
        "status=active",
        "x-request-id"
      );
      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users/{userId}/orders",
        List.of(telemetryData)
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldNotThrow_whenParameterHasNullIn() {
      var parameter = new Parameter();
      parameter.setName("someParam");
      // in is intentionally left null to reproduce the NPE scenario

      var operation = new Operation();
      operation.setParameters(List.of(parameter));

      var pathToOpenAPIOperationMap = Map.of("GET_/api/v1/users", operation);

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "someParam=value")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(PARAMETER_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r -> assertThat(r.additionalInformation()).isNotNull()
      );
    }

    @Test
    void shouldHandleOperationWithNullParameters() {
      var operation = new Operation();
      operation.setParameters(null);

      var pathToOpenAPIOperationMap = Map.of("GET_/api/v1/users", operation);

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "page=1")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldNotCoverParameter_whenTelemetryAttributesAreNull() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("page", "query", false))
        )
      );

      var telemetryData = new OpenTelemetryData("span-123", "trace-456", null);
      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetryData)
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(0.0));
    }

    @Test
    void shouldNotCoverParameter_whenParameterLocationIsUnsupported() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("session", "cookie", false))
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "page=1")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(0.0));
    }

    @Test
    void shouldNotCoverQueryParameter_whenQueryAttributeIsAbsent() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("page", "query", false))
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(0.0));
    }

    @Test
    void shouldNotCoverQueryParameter_whenQueryStringIsEmpty() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("page", "query", false))
        )
      );

      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("url.query", "");
      var telemetryData = new OpenTelemetryData(
        "span-123",
        "trace-456",
        attributes
      );
      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetryData)
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(0.0));
    }

    @Test
    void shouldCoverQueryParameter_whenPresentAsBareFlagToken() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("debug", "query", false))
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "page=1&debug")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(1.0));
    }

    private Map<String, Operation> createOperationsWithParameters(
      Map<String, List<Parameter>> pathToParameters
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<Parameter>
      > entry : pathToParameters.entrySet()) {
        Operation operation = new Operation();
        if (!entry.getValue().isEmpty()) {
          operation.setParameters(new ArrayList<>(entry.getValue()));
        }
        result.put(entry.getKey(), operation);
      }

      return result;
    }

    private Parameter createParameter(
      String name,
      String in,
      boolean required
    ) {
      Parameter param = new Parameter();
      param.setName(name);
      param.setIn(in);
      param.setRequired(required);
      return param;
    }

    private Map<String, List<OpenTelemetryData>> createTelemetryWithQueryParams(
      Map<String, String> pathToQueryString
    ) {
      Map<String, List<OpenTelemetryData>> result = new HashMap<>();

      for (Map.Entry<String, String> entry : pathToQueryString.entrySet()) {
        var attributes = JsonMapper.shared().createObjectNode();
        if (!entry.getValue().isEmpty()) {
          attributes.put("url.query", entry.getValue());
        }

        var telemetryData = new OpenTelemetryData(
          "span-123",
          "trace-456",
          attributes
        );
        result.put(entry.getKey(), List.of(telemetryData));
      }

      return result;
    }

    private Map<String, List<OpenTelemetryData>> createTelemetryWithHeader(
      Map<String, String> pathToHeader
    ) {
      Map<String, List<OpenTelemetryData>> result = new HashMap<>();

      for (Map.Entry<String, String> entry : pathToHeader.entrySet()) {
        var attributes = JsonMapper.shared().createObjectNode();
        attributes.put(
          "http.request.header." + entry.getValue().toLowerCase(ROOT),
          "some-value"
        );

        var telemetryData = new OpenTelemetryData(
          "span-123",
          "trace-456",
          attributes
        );
        result.put(entry.getKey(), List.of(telemetryData));
      }

      return result;
    }

    private OpenTelemetryData createTelemetryDataWithMultipleParams(
      String queryString,
      String headerName
    ) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("url.query", queryString);
      attributes.put(
        "http.request.header." + headerName.toLowerCase(ROOT),
        "some-value"
      );

      return new OpenTelemetryData("span-123", "trace-456", attributes);
    }

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, HALF_UP);
    }
  }

  @Nested
  class CalculateFindingsTest {

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldReturnOneFindingPerDeclaredParameterInDocumentOrder() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWith(
          parameter("page", "query", false),
          parameter("size", "query", false)
        )
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetryWithQuery("trace-1", "page=1"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .extracting(
          ApiTestFinding::specPointer,
          ApiTestFinding::status,
          ApiTestFinding::httpPath,
          ApiTestFinding::httpMethod,
          ApiTestFinding::parameterName
        )
        .containsExactly(
          tuple(
            "/paths/~1api~1v1~1users/get/parameters/0",
            COVERED,
            "/api/v1/users",
            "GET",
            "page"
          ),
          tuple(
            "/paths/~1api~1v1~1users/get/parameters/1",
            UNCOVERED,
            "/api/v1/users",
            "GET",
            "size"
          )
        );
    }

    @Test
    void shouldOrderTheFindingsOfSeveralOperationsByOperationKey() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWith(parameter("dryRun", "query", false)),
        "GET_/api/v1/users",
        operationWith(parameter("page", "query", false))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .extracting(ApiTestFinding::specPointer)
        .containsExactly(
          "/paths/~1api~1v1~1users/get/parameters/0",
          "/paths/~1api~1v1~1users/post/parameters/0"
        );
    }

    @Test
    void shouldContributeNoTargetForAnOperationWithoutParameters() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/health",
        new Operation(),
        "GET_/api/v1/users",
        operationWith(parameter("page", "query", false))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .singleElement()
        .extracting(ApiTestFinding::parameterName)
        .isEqualTo("page");
    }

    @Test
    void shouldJudgeEveryDeclaredParameterRegardlessOfWhetherItIsRequired() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWith(
          parameter("page", "query", false),
          parameter("tenant", "query", true)
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .extracting(ApiTestFinding::status)
        .containsExactly(UNCOVERED, UNCOVERED);
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
    void shouldEvidenceOnlyTheSpansThatCarriedTheParameter() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWith(parameter("page", "query", false))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          telemetryWithQuery("trace-without", "size=10"),
          telemetryWithQuery("trace-with", "page=1&size=10")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .extracting(ApiTestFinding::evidence, as(list(FindingEvidence.class)))
        .containsExactly(new FindingEvidence("trace-with", null));
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_004_PARAMETER_COVERAGE_MATCHES_BY_TOKEN_NOT_SUBSTRING
    )
    void shouldNotEvidenceAParameterNameThatOnlyAppearsInsideAnotherToken() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWith(parameter("page", "query", false))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetryWithQuery("trace-1", "pageSize=10&sort=page"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(UNCOVERED),
          finding -> assertThat(finding.evidence()).isEmpty()
        );
    }

    @Test
    void shouldLeaveTheResponseCodeAndContentTypeUnset() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWith(parameter("page", "query", false))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.responseCode()).isNull(),
          finding -> assertThat(finding.contentType()).isNull()
        );
    }

    private Operation operationWith(Parameter... parameters) {
      var operation = new Operation();
      operation.setParameters(new ArrayList<>(List.of(parameters)));
      return operation;
    }

    private Parameter parameter(String name, String in, boolean required) {
      var parameter = new Parameter();
      parameter.setName(name);
      parameter.setIn(in);
      parameter.setRequired(required);
      return parameter;
    }

    private OpenTelemetryData telemetryWithQuery(
      String traceId,
      String queryString
    ) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("url.query", queryString);
      return new OpenTelemetryData("span-1", traceId, attributes);
    }
  }
}

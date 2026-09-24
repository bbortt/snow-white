/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_PARAMETER_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.UNCOVERED;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

import clew.traceables.clew.SwTraceables;
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
class RequiredParameterCoverageCalculatorUnitTest {

  private RequiredParameterCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new RequiredParameterCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenRequiredParameterCoverage() {
      boolean result = fixture.accepts(REQUIRED_PARAMETER_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotRequiredParameterCoverage(
      OpenApiCoverageCriteria openApiCriteria
    ) {
      if (REQUIRED_PARAMETER_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllRequiredParametersCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(
            createParameter("userId", "query", true),
            createParameter("page", "query", false),
            createParameter("size", "query", false)
          )
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "userId=123")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            REQUIRED_PARAMETER_COVERAGE
          ),
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
    void shouldIgnoreOptionalParameters() {
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
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            REQUIRED_PARAMETER_COVERAGE
          ),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn0Percent_whenRequiredParametersNotCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(
            createParameter("userId", "query", true),
            createParameter("organizationId", "query", true)
          )
        )
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            REQUIRED_PARAMETER_COVERAGE
          ),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r -> assertThat(r.additionalInformation()).isNotNull()
      );
    }

    @Test
    void shouldReturn50Percent_whenSomeRequiredParametersCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(
            createParameter("userId", "query", true),
            createParameter("organizationId", "query", true),
            createParameter("page", "query", false)
          )
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of("GET_/api/v1/users", "userId=123&page=1")
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            REQUIRED_PARAMETER_COVERAGE
          ),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following required parameters are uncovered: `GET_/api/v1/users [query: organizationId]`"
          )
      );
    }

    @Test
    void shouldHandleRequiredPathParameters() {
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
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldHandleMultipleOperations() {
      var pathToOpenAPIOperationMap = createOperationsWithParameters(
        Map.of(
          "GET_/api/v1/users",
          List.of(createParameter("userId", "query", true)),
          "POST_/api/v1/users",
          List.of(createParameter("name", "query", true))
        )
      );

      var pathToTelemetryMap = createTelemetryWithQueryParams(
        Map.of(
          "GET_/api/v1/users",
          "userId=123",
          "POST_/api/v1/users",
          "name=John"
        )
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

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, HALF_UP);
    }
  }

  @Nested
  class CalculateFindingsTest {

    @Test
    @VerifiesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
    void shouldRecordTheOptionalSiblingAsNotApplicableRatherThanDropIt() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWith(
          parameter("tenant", "query", true),
          parameter("page", "query", false)
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .extracting(
          ApiTestFinding::specPointer,
          ApiTestFinding::status,
          ApiTestFinding::parameterName
        )
        .containsExactly(
          tuple(
            "/paths/~1api~1v1~1users/get/parameters/0",
            UNCOVERED,
            "tenant"
          ),
          tuple(
            "/paths/~1api~1v1~1users/get/parameters/1",
            NOT_APPLICABLE,
            "page"
          )
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
    void shouldCarryNoEvidenceOnTheOptionalSiblingEvenWhenItWasExercised() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWith(
          parameter("tenant", "query", true),
          parameter("page", "query", false)
        )
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetryWithQuery("trace-1", "tenant=acme&page=1"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .extracting(ApiTestFinding::status, ApiTestFinding::evidence)
        .containsExactly(
          tuple(COVERED, List.of(new FindingEvidence("trace-1", null))),
          tuple(NOT_APPLICABLE, List.of())
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

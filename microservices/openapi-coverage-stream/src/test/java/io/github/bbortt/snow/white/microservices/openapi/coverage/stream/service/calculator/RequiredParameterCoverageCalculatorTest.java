/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_PARAMETER_COVERAGE;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
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
class RequiredParameterCoverageCalculatorTest {

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
      OpenApiCriteria openApiCriteria
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
}

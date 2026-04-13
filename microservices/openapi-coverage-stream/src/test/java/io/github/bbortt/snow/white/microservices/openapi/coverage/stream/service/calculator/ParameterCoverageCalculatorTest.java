/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.PARAMETER_COVERAGE;
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
class ParameterCoverageCalculatorTest {

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
      OpenApiCriteria openApiCriteria
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
          "http.request.header." + entry.getValue().toLowerCase(),
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
        "http.request.header." + headerName.toLowerCase(),
        "some-value"
      );

      return new OpenTelemetryData("span-123", "trace-456", attributes);
    }

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, HALF_UP);
    }
  }
}

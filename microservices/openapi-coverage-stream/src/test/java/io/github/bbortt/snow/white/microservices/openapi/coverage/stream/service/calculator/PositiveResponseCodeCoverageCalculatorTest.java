/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.POSITIVE_RESPONSE_CODE_COVERAGE;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.math.BigDecimal;
import java.time.Duration;
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
class PositiveResponseCodeCoverageCalculatorTest {

  private PositiveResponseCodeCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new PositiveResponseCodeCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenPositiveResponseCodeCoverage() {
      boolean result = fixture.accepts(POSITIVE_RESPONSE_CODE_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotPositiveResponseCodeCoverage(
      OpenApiCriteria openApiCriteria
    ) {
      if (POSITIVE_RESPONSE_CODE_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllPositiveCodesCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithPositiveCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("200", "201"),
          "POST_/api/v1/users",
          List.of("201", "302")
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("200", "201"),
          "POST_/api/v1/users",
          List.of("201", "302")
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            POSITIVE_RESPONSE_CODE_COVERAGE
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
    void shouldReturn50Percent_whenSomePositiveCodesCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithPositiveCodes(
        Map.of("GET_/api/v1/users", List.of("200", "201"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200")) // Missing 201
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            POSITIVE_RESPONSE_CODE_COVERAGE
          ),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following positive response codes in paths are uncovered: `GET_/api/v1/users [201]`"
          )
      );
    }

    @Test
    void shouldReturn0Percent_whenNoPositiveCodesCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithPositiveCodes(
        Map.of("GET_/api/v1/users", List.of("200", "204"))
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            POSITIVE_RESPONSE_CODE_COVERAGE
          ),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following positive response codes in paths are uncovered: `GET_/api/v1/users [200]`, `GET_/api/v1/users [204]`"
          )
      );
    }

    @Test
    void shouldIgnoreErrorStatusCodes_whenPresentInTelemetry() {
      var pathToOpenAPIOperationMap = createOperationsWithPositiveCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "400", "500"))
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
    void shouldHandlePatternPositiveCodes() {
      var pathToOpenAPIOperationMap = createOperationsWithPositiveCodes(
        Map.of("GET_/api/v1/users", List.of("2XX", "3XX"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "301"))
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
    void shouldHandleOperationWithOnlyErrorResponses() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodesOnly(
        Map.of("DELETE_/api/v1/users/{id}", List.of("400", "404", "500"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("DELETE_/api/v1/users/{id}", List.of("400"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)), // No positive codes to cover
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldHandleNullResponses() {
      var operationWithNullResponses = new Operation();
      operationWithNullResponses.setResponses(null);

      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithNullResponses
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
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

    private Map<String, Operation> createOperationsWithPositiveCodes(
      Map<String, List<String>> pathToPositiveCodes
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<String>
      > entry : pathToPositiveCodes.entrySet()) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();

        for (String positiveCode : entry.getValue()) {
          responses.addApiResponse(
            positiveCode,
            new ApiResponse().description("Success")
          );
        }

        // Always add an error response to verify it is excluded
        responses.addApiResponse(
          "500",
          new ApiResponse().description("Internal Server Error")
        );

        operation.setResponses(responses);
        result.put(entry.getKey(), operation);
      }

      return result;
    }

    private Map<String, Operation> createOperationsWithErrorCodesOnly(
      Map<String, List<String>> pathToErrorCodes
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<String>
      > entry : pathToErrorCodes.entrySet()) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();

        for (String errorCode : entry.getValue()) {
          responses.addApiResponse(
            errorCode,
            new ApiResponse().description("Error")
          );
        }

        operation.setResponses(responses);
        result.put(entry.getKey(), operation);
      }

      return result;
    }

    private Map<String, List<OpenTelemetryData>> createTelemetryWithStatusCodes(
      Map<String, List<String>> pathToStatusCodes
    ) {
      Map<String, List<OpenTelemetryData>> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<String>
      > entry : pathToStatusCodes.entrySet()) {
        List<OpenTelemetryData> telemetryList = entry
          .getValue()
          .stream()
          .map(statusCode ->
            createTelemetryDataWithAttribute(
              "http.response.status_code",
              statusCode
            )
          )
          .toList();

        result.put(entry.getKey(), telemetryList);
      }

      return result;
    }

    private OpenTelemetryData createTelemetryDataWithAttribute(
      String attributeName,
      String value
    ) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put(attributeName, value);

      return new OpenTelemetryData("span-123", "trace-456", attributes);
    }

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, HALF_UP);
    }
  }
}

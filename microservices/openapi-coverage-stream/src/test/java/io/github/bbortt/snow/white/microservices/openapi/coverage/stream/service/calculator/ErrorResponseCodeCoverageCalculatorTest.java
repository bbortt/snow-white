/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.ERROR_RESPONSE_CODE_COVERAGE;
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
class ErrorResponseCodeCoverageCalculatorTest {

  private ErrorResponseCodeCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ErrorResponseCodeCoverageCalculator();
  }

  @Nested
  class Accepts {

    @Test
    void shouldReturnTrue_whenErrorResponseCodeCoverage() {
      boolean result = fixture.accepts(ERROR_RESPONSE_CODE_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource(OpenApiCriteria.class)
    @ParameterizedTest
    void shouldReturnFalse_whenNotPathCoverage(
      OpenApiCriteria openApiCriteria
    ) {
      if (ERROR_RESPONSE_CODE_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class Calculates {

    @Test
    void shouldReturn100Percent_whenAllErrorCodesCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("400", "404", "500"),
          "POST_/api/v1/users",
          List.of("400", "422")
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("400", "404", "500"),
          "POST_/api/v1/users",
          List.of("400", "422")
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            ERROR_RESPONSE_CODE_COVERAGE
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
    void shouldReturn60Percent_whenSomeErrorCodesCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("400", "404", "500"),
          "POST_/api/v1/users",
          List.of("400", "422")
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("400", "404"), // Missing 500
          "POST_/api/v1/users",
          List.of("400") // Missing 422
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            ERROR_RESPONSE_CODE_COVERAGE
          ),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.6)), // 3/5 covered
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following error codes in paths are uncovered: `GET_/api/v1/users [500]`, `POST_/api/v1/users [422]`"
          )
      );
    }

    @Test
    void shouldReturn0Percent_whenNoErrorCodesCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("400", "404"),
          "POST_/api/v1/users",
          List.of("500")
        )
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>(); // No telemetry data

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            ERROR_RESPONSE_CODE_COVERAGE
          ),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following error codes in paths are uncovered: `GET_/api/v1/users [400]`, `GET_/api/v1/users [404]`, `POST_/api/v1/users [500]`"
          )
      );
    }

    @Test
    void shouldIgnoreSuccessStatusCodes_whenPresentInTelemetry() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of("GET_/api/v1/users", List.of("400", "404"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "201", "400", "404"))
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
    void shouldHandlePatternErrorCodes() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of("GET_/api/v1/users", List.of("4XX", "5XX"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("400", "500"))
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
    void shouldIgnoreDefaultErrorCode() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of("GET_/api/v1/users", List.of("default"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("500"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following default response codes in paths were being ignored for the calculation: `GET_/api/v1/users [default]`"
          )
      );
    }

    @Test
    void shouldHandleOperationsWithoutResponseCodes() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of("GET_/api/v1/users", List.of()) // No response codes defined
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("400", "500"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)), // 0/0 = 100%
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldHandleEmptyOperationsMap() {
      var pathToOpenAPIOperationMap = new HashMap<String, Operation>();
      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("400", "500"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)), // 0/0 = 100%
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldHandleTelemetryWithoutStatusCode() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of("GET_/api/v1/users", List.of("400", "404"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(createTelemetryDataWithoutStatusCode())
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following error codes in paths are uncovered: `GET_/api/v1/users [400]`, `GET_/api/v1/users [404]`"
          )
      );
    }

    @Test
    void shouldHandleMixedErrorCodeFormats() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorCodes(
        Map.of("GET_/api/v1/users", List.of("400", "4XX", "default"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("400", "422", "500"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal((double) 2 / 3)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following default response codes in paths were being ignored for the calculation: `GET_/api/v1/users [default]`"
          )
      );
    }

    @Test
    void shouldHandleOperationWithOnlySuccessResponses() {
      var pathToOpenAPIOperationMap = createOperationsWithSuccessCodesOnly(
        Map.of("GET_/api/v1/users", List.of("200", "201"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "400"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)), // No error codes to cover
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
        Map.of("GET_/api/v1/users", List.of("400"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)), // No error codes to cover
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    private Map<String, Operation> createOperationsWithErrorCodes(
      Map<String, List<String>> pathToErrorCodes
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<String>
      > entry : pathToErrorCodes.entrySet()) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();

        // Add success response
        responses.addApiResponse(
          "200",
          new ApiResponse().description("Success")
        );

        // Add error responses
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

    private Map<String, Operation> createOperationsWithSuccessCodesOnly(
      Map<String, List<String>> pathToSuccessCodes
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<String>
      > entry : pathToSuccessCodes.entrySet()) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();

        // Add only success responses
        for (String successCode : entry.getValue()) {
          responses.addApiResponse(
            successCode,
            new ApiResponse().description("Success")
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

    private OpenTelemetryData createTelemetryDataWithoutStatusCode() {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("some.other.attribute", "value");

      return new OpenTelemetryData("span-123", "trace-456", attributes);
    }

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, HALF_UP);
    }
  }
}

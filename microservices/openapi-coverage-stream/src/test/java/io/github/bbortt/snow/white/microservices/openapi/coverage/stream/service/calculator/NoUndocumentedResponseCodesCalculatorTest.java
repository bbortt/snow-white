/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.NO_UNDOCUMENTED_RESPONSE_CODES;
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
class NoUndocumentedResponseCodesCalculatorTest {

  private NoUndocumentedResponseCodesCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new NoUndocumentedResponseCodesCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenNoUndocumentedResponseCodes() {
      boolean result = fixture.accepts(NO_UNDOCUMENTED_RESPONSE_CODES);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotNoUndocumentedResponseCodes(
      OpenApiCriteria openApiCriteria
    ) {
      if (NO_UNDOCUMENTED_RESPONSE_CODES.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllObservedCodesAreDocumented() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200", "400", "500"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "400"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            NO_UNDOCUMENTED_RESPONSE_CODES
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
    void shouldReturn0Percent_whenObservedCodeIsNotDocumented() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("500"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            NO_UNDOCUMENTED_RESPONSE_CODES
          ),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes are not documented in the OpenAPI specification: `GET_/api/v1/users [500]`"
          )
      );
    }

    @Test
    void shouldReturn50Percent_whenHalfObservedCodesAreDocumented() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200", "400"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "500"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes are not documented in the OpenAPI specification: `GET_/api/v1/users [500]`"
          )
      );
    }

    @Test
    void shouldMatchWildcardPatterns() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("2XX", "4XX", "5XX"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "201", "404", "500"))
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
    void shouldHandleDefaultCatchAll() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200", "default"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "401", "403", "500"))
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
    void shouldReturn100Percent_whenNoTelemetryData() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200", "400"))
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

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
    void shouldHandleOperationWithNoSpecifiedResponses() {
      var operation = new Operation();
      operation.setResponses(null);

      var pathToOpenAPIOperationMap = Map.of("GET_/api/v1/users", operation);

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes are not documented in the OpenAPI specification: `GET_/api/v1/users [200]`"
          )
      );
    }

    @Test
    void shouldHandleMultipleOperations() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("200"),
          "POST_/api/v1/users",
          List.of("201", "400")
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("200"),
          "POST_/api/v1/users",
          List.of("201", "500")
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.67)),
        r ->
          assertThat(r.additionalInformation()).contains(
            "POST_/api/v1/users [500]"
          )
      );
    }

    @Test
    void shouldReturn100Percent_whenTelemetryUsesConcretePathForTemplate() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/pung/{message}", List.of("200", "400", "500"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/pung/hello", List.of("200"))
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
    void shouldHandleTelemetryWithoutStatusCode() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var telemetryData = createTelemetryDataWithoutStatusCode();
      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
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

    private Map<String, Operation> createOperationsWithResponseCodes(
      Map<String, List<String>> pathToResponseCodes
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<String>
      > entry : pathToResponseCodes.entrySet()) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();

        for (String code : entry.getValue()) {
          responses.addApiResponse(code, new ApiResponse().description("Test"));
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

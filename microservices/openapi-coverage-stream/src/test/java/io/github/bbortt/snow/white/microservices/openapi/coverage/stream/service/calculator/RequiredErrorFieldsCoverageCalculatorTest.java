/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCriteria;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
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
class RequiredErrorFieldsCoverageCalculatorTest {

  private RequiredErrorFieldsCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new RequiredErrorFieldsCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenRequiredErrorFieldsCoverage() {
      boolean result = fixture.accepts(REQUIRED_ERROR_FIELDS_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotRequiredErrorFieldsCoverage(
      OpenApiCriteria openApiCriteria
    ) {
      if (REQUIRED_ERROR_FIELDS_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenErrorResponsesWithRequiredFieldsAreCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of(
          "GET_/api/v1/users",
          Map.of("400", List.of("message", "code"), "500", List.of("message"))
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("400", "500"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(
            REQUIRED_ERROR_FIELDS_COVERAGE
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
    void shouldReturn0Percent_whenNoErrorResponsesObserved() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of("GET_/api/v1/users", Map.of("400", List.of("message", "code")))
      );

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
            "The following error responses with required fields are not covered: `GET_/api/v1/users [400: fields=code, message]`"
          )
      );
    }

    @Test
    void shouldReturn50Percent_whenHalfErrorResponsesAreCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of(
          "GET_/api/v1/users",
          Map.of("400", List.of("message"), "500", List.of("error"))
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("400"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r -> assertThat(r.additionalInformation()).contains("500: fields=error")
      );
    }

    @Test
    void shouldReturn100Percent_whenNoErrorSchemasHaveRequiredFields() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of("GET_/api/v1/users", Map.of("400", List.of()))
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

    @Test
    void shouldMatchWildcardErrorCodes() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of(
          "GET_/api/v1/users",
          Map.of("4XX", List.of("message"), "5XX", List.of("error"))
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("404", "503"))
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
    void shouldHandleDefaultErrorResponse() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of("GET_/api/v1/users", Map.of("default", List.of("message")))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("500"))
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
    void shouldIgnoreSuccessResponsesWithRequiredFields() {
      var operationMap = createOperationsWithSuccessAndErrorSchemas(
        "GET_/api/v1/users",
        Map.of("200", List.of("data", "total")),
        Map.of("400", List.of("message"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("400"))
      );

      OpenApiTestResult result = fixture.calculate(
        operationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(1.0)),
        r -> assertThat(r.additionalInformation()).isNull()
      );
    }

    @Test
    void shouldReturn100Percent_whenNoTelemetryData() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of("GET_/api/v1/users", Map.of("400", List.of("message")))
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r -> assertThat(r.additionalInformation()).isNotNull()
      );
    }

    @Test
    void shouldHandleOperationWithNullResponses() {
      var operation = new Operation();
      operation.setResponses(null);

      var pathToOpenAPIOperationMap = Map.of("GET_/api/v1/users", operation);

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("400"))
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
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of(
          "GET_/api/v1/users",
          Map.of("400", List.of("message")),
          "POST_/api/v1/users",
          Map.of("422", List.of("errors"))
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("400"),
          "POST_/api/v1/users",
          List.of("201")
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).contains(
            "POST_/api/v1/users [422: fields=errors]"
          )
      );
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<String, Operation> createOperationsWithErrorSchemas(
      Map<String, Map<String, List<String>>> pathToErrorSchemas
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        Map<String, List<String>>
      > pathEntry : pathToErrorSchemas.entrySet()) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();

        for (Map.Entry<String, List<String>> schemaEntry : pathEntry
          .getValue()
          .entrySet()) {
          String statusCode = schemaEntry.getKey();
          List<String> requiredFields = schemaEntry.getValue();

          ApiResponse apiResponse = new ApiResponse();
          apiResponse.setDescription("Test error response");

          if (!requiredFields.isEmpty()) {
            Content content = new Content();
            MediaType mediaType = new MediaType();
            Schema schema = new Schema();
            schema.setRequired(requiredFields);
            mediaType.setSchema(schema);
            content.addMediaType("application/json", mediaType);
            apiResponse.setContent(content);
          }

          responses.addApiResponse(statusCode, apiResponse);
        }

        operation.setResponses(responses);
        result.put(pathEntry.getKey(), operation);
      }

      return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<String, Operation> createOperationsWithSuccessAndErrorSchemas(
      String operationKey,
      Map<String, List<String>> successSchemas,
      Map<String, List<String>> errorSchemas
    ) {
      Operation operation = new Operation();
      ApiResponses responses = new ApiResponses();

      for (Map.Entry<
        String,
        List<String>
      > schemaEntry : successSchemas.entrySet()) {
        String statusCode = schemaEntry.getKey();
        List<String> requiredFields = schemaEntry.getValue();

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("Success response");

        if (!requiredFields.isEmpty()) {
          Content content = new Content();
          MediaType mediaType = new MediaType();
          Schema schema = new Schema();
          schema.setRequired(requiredFields);
          mediaType.setSchema(schema);
          content.addMediaType("application/json", mediaType);
          apiResponse.setContent(content);
        }

        responses.addApiResponse(statusCode, apiResponse);
      }

      for (Map.Entry<
        String,
        List<String>
      > schemaEntry : errorSchemas.entrySet()) {
        String statusCode = schemaEntry.getKey();
        List<String> requiredFields = schemaEntry.getValue();

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription("Error response");

        if (!requiredFields.isEmpty()) {
          Content content = new Content();
          MediaType mediaType = new MediaType();
          Schema schema = new Schema();
          schema.setRequired(requiredFields);
          mediaType.setSchema(schema);
          content.addMediaType("application/json", mediaType);
          apiResponse.setContent(content);
        }

        responses.addApiResponse(statusCode, apiResponse);
      }

      operation.setResponses(responses);
      return Map.of(operationKey, operation);
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

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.REQUIRED_ERROR_FIELDS_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.NOT_APPLICABLE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.UNCOVERED;
import static java.math.RoundingMode.HALF_UP;
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
class RequiredErrorFieldsCoverageCalculatorUnitTest {

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
      OpenApiCoverageCriteria openApiCriteria
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
    @VerifiesSw(SwTraceables.SW_006_REQUIRED_ERROR_FIELDS_COVERAGE)
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
    @VerifiesSw(SwTraceables.SW_006_REQUIRED_ERROR_FIELDS_COVERAGE)
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

    @Test
    void shouldIgnoreMediaTypeWithNullSchema() {
      var operation = new Operation();
      var responses = new ApiResponses();

      var apiResponse = new ApiResponse();
      var content = new Content();
      var mediaType = new MediaType();
      mediaType.setSchema(null);
      content.addMediaType("application/json", mediaType);
      apiResponse.setContent(content);

      responses.addApiResponse("400", apiResponse);
      operation.setResponses(responses);

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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void shouldIgnoreSchemaWithNullRequiredFields() {
      var operation = new Operation();
      var responses = new ApiResponses();

      var apiResponse = new ApiResponse();
      var content = new Content();
      var mediaType = new MediaType();
      var schema = new Schema();
      schema.setRequired(null);
      mediaType.setSchema(schema);
      content.addMediaType("application/json", mediaType);
      apiResponse.setContent(content);

      responses.addApiResponse("400", apiResponse);
      operation.setResponses(responses);

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
    void shouldIgnoreTelemetryWithNullAttributes() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of("GET_/api/v1/users", Map.of("400", List.of("message")))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(new OpenTelemetryData("span-123", "trace-456", null))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(0.0));
    }

    @Test
    void shouldIgnoreTelemetryMissingStatusCodeAttribute() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of("GET_/api/v1/users", Map.of("400", List.of("message")))
      );

      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("other.attribute", "value");

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(new OpenTelemetryData("span-123", "trace-456", attributes))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(0.0));
    }

    @Test
    void shouldNotMatchWildcardWhenNoErrorCodesObserved() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of("GET_/api/v1/users", Map.of("4XX", List.of("message")))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(0.0));
    }

    @Test
    void shouldNotCoverDefaultResponseWhenNoErrorsObserved() {
      var pathToOpenAPIOperationMap = createOperationsWithErrorSchemas(
        Map.of("GET_/api/v1/users", Map.of("default", List.of("message")))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result.coverage()).isEqualTo(getBigDecimal(0.0));
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

  @Nested
  class CalculateFindingsTest {

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    @VerifiesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
    void shouldReturnOneFindingPerDocumentedResponseEntry() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithResponses(
          response("200", List.of()),
          response("404", List.of("code", "message")),
          response("500", List.of())
        )
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetry("trace-404", "404"))
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
          ApiTestFinding::responseCode
        )
        .containsExactly(
          tuple(
            "/paths/~1api~1v1~1users/get/responses/200",
            NOT_APPLICABLE,
            "/api/v1/users",
            "GET",
            "200"
          ),
          tuple(
            "/paths/~1api~1v1~1users/get/responses/404",
            COVERED,
            "/api/v1/users",
            "GET",
            "404"
          ),
          tuple(
            "/paths/~1api~1v1~1users/get/responses/500",
            NOT_APPLICABLE,
            "/api/v1/users",
            "GET",
            "500"
          )
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
    void shouldCarryNoEvidenceOnAnEntryThisCriterionDoesNotJudge() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithResponses(response("500", List.of()))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetry("trace-500", "500"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(NOT_APPLICABLE),
          finding -> assertThat(finding.evidence()).isEmpty()
        );
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
    void shouldEvidenceOnlyTheSpansThatExhibitedTheDocumentedCode() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithResponses(
          response("404", List.of("code")),
          response("500", List.of("code"))
        )
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetry("trace-404", "404"), telemetry("trace-500", "500"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "500".equals(finding.responseCode()))
        .singleElement()
        .extracting(ApiTestFinding::evidence, as(list(FindingEvidence.class)))
        .containsExactly(new FindingEvidence("trace-500", null));
    }

    @Test
    @VerifiesSw(SwTraceables.SW_006_REQUIRED_ERROR_FIELDS_COVERAGE)
    void shouldEvidenceAWildcardEntryFromAnyCodeSharingItsLeadingDigit() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithResponses(response("4XX", List.of("code")))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetry("trace-500", "500"), telemetry("trace-418", "418"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(COVERED),
          finding ->
            assertThat(finding.evidence()).containsExactly(
              new FindingEvidence("trace-418", null)
            )
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_006_REQUIRED_ERROR_FIELDS_COVERAGE)
    void shouldEvidenceADefaultEntryFromEveryObservedErrorCode() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithResponses(response("default", List.of("code")))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          telemetry("trace-ok", "200"),
          telemetry("trace-404", "404"),
          telemetry("trace-500", "500")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .extracting(ApiTestFinding::evidence, as(list(FindingEvidence.class)))
        .containsExactly(
          new FindingEvidence("trace-404", null),
          new FindingEvidence("trace-500", null)
        );
    }

    @Test
    void shouldCarryNoEvidenceOnAnUncoveredFinding() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithResponses(response("404", List.of("code")))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetry("trace-500", "500"))
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
    void shouldLeaveTheParameterNameAndContentTypeUnset() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithResponses(response("404", List.of("code")))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.parameterName()).isNull(),
          finding -> assertThat(finding.contentType()).isNull()
        );
    }

    @SafeVarargs
    private Operation operationWithResponses(
      Map.Entry<String, ApiResponse>... entries
    ) {
      var responses = new ApiResponses();
      for (Map.Entry<String, ApiResponse> entry : entries) {
        responses.addApiResponse(entry.getKey(), entry.getValue());
      }

      var operation = new Operation();
      operation.setResponses(responses);
      return operation;
    }

    private Map.Entry<String, ApiResponse> response(
      String statusCode,
      List<String> requiredFields
    ) {
      var apiResponse = new ApiResponse();

      if (!requiredFields.isEmpty()) {
        var schema = new Schema<>();
        schema.setRequired(requiredFields);

        var mediaType = new MediaType();
        mediaType.setSchema(schema);

        var content = new Content();
        content.addMediaType("application/json", mediaType);
        apiResponse.setContent(content);
      }

      return Map.entry(statusCode, apiResponse);
    }

    private OpenTelemetryData telemetry(String traceId, String statusCode) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("http.response.status_code", statusCode);
      return new OpenTelemetryData("span-1", traceId, attributes);
    }
  }
}

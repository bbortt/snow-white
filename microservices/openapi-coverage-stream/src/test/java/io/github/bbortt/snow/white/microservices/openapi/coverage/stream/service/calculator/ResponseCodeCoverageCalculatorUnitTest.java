/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.RESPONSE_CODE_COVERAGE;
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
import io.github.bbortt.snow.white.commons.event.dto.ApiTestFinding;
import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.commons.event.dto.OpenApiTestResult;
import io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria;
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
class ResponseCodeCoverageCalculatorUnitTest {

  private ResponseCodeCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ResponseCodeCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenErrorResponseCodeCoverage() {
      boolean result = fixture.accepts(RESPONSE_CODE_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotPathCoverage(
      OpenApiCoverageCriteria openApiCriteria
    ) {
      if (RESPONSE_CODE_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllErrorCodesCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
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
        r -> assertThat(r.openApiCriteria()).isEqualTo(RESPONSE_CODE_COVERAGE),
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
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
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
        r -> assertThat(r.openApiCriteria()).isEqualTo(RESPONSE_CODE_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.6)), // 3/5 covered
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes in paths are uncovered: `GET_/api/v1/users [500]`, `POST_/api/v1/users [422]`"
          )
      );
    }

    @Test
    void shouldReturn0Percent_whenNoCodesCovered() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("200", "301", "400", "404"),
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
        r -> assertThat(r.openApiCriteria()).isEqualTo(RESPONSE_CODE_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes in paths are uncovered: `GET_/api/v1/users [200]`, `GET_/api/v1/users [301]`, `GET_/api/v1/users [400]`, `GET_/api/v1/users [404]`, `POST_/api/v1/users [500]`"
          )
      );
    }

    @Test
    void shouldIgnoreSuccessStatusCodes_whenPresentInTelemetry() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
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
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
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
    @VerifiesSw(
      SwTraceables.SW_002_RESPONSE_CODE_COVERAGE_TREATS_DEFAULT_AS_WILDCARD
    )
    void shouldCoverDefaultResponseCode_whenObservedStatusFallsOutsideMoreSpecificEntries() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200", "default"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "404"))
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
    @VerifiesSw(
      SwTraceables.SW_002_RESPONSE_CODE_COVERAGE_TREATS_DEFAULT_AS_WILDCARD
    )
    void shouldLeaveDefaultResponseCodeUncovered_whenEveryObservedStatusMatchesAMoreSpecificEntry() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200", "default"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following default response codes in paths are uncovered, as no observed status code fell outside a more specific documented response: `GET_/api/v1/users [default]`"
          )
      );
    }

    @Test
    void shouldHandleOperationsWithoutResponseCodes() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of()) // No error codes defined
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
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
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
            "The following response codes in paths are uncovered: `GET_/api/v1/users [400]`, `GET_/api/v1/users [404]`"
          )
      );
    }

    @Test
    void shouldHandleMixedErrorCodeFormats() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of(
          "GET_/api/v1/users",
          List.of("2XX", "300", "400", "4XX", "default")
        )
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("202", "301", "400", "422", "500"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        // "300" is the only entry left uncovered: 2XX/400/4XX each match an observed code, and
        // the observed "301" falls outside every entry but "default", covering it too.
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.8)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes in paths are uncovered: `GET_/api/v1/users [300]`"
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
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes in paths are uncovered: `GET_/api/v1/users [201]`"
          )
      );
    }

    @Test
    void shouldReturn100Percent_whenTelemetryUsesConcretePathForTemplate() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/pung/{message}", List.of("200", "400", "500"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/pung/hello", List.of("200", "400", "500"))
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

    private Map<String, Operation> createOperationsWithCodes(
      Map<String, List<String>> pathToErrorCodes
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<String>
      > entry : pathToErrorCodes.entrySet()) {
        Operation operation = new Operation();
        ApiResponses responses = new ApiResponses();

        // Add responses
        for (String errorCode : entry.getValue()) {
          responses.addApiResponse(
            errorCode,
            new ApiResponse().description("Test")
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

  @Nested
  class CalculateFindingsTest {

    @Test
    void shouldReturnOneFindingPerDocumentedEntryOrderedByOperationThenCode() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of(
          "POST_/api/v1/users",
          List.of("500", "201"),
          "GET_/api/v1/users",
          List.of("404", "200")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        new HashMap<>()
      );

      assertThat(result)
        .extracting(ApiTestFinding::httpMethod, ApiTestFinding::responseCode)
        .containsExactly(
          tuple("GET", "200"),
          tuple("GET", "404"),
          tuple("POST", "201"),
          tuple("POST", "500")
        );
    }

    @Test
    void shouldNameTheResponseEntryAndDenormalizeItsDiscriminators() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/pung/{message}", List.of("404"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        new HashMap<>()
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding ->
            assertThat(finding.specPointer()).isEqualTo(
              "/paths/~1pung~1{message}/get/responses/404"
            ),
          finding ->
            assertThat(finding.httpPath()).isEqualTo("/pung/{message}"),
          finding -> assertThat(finding.httpMethod()).isEqualTo("GET"),
          finding -> assertThat(finding.responseCode()).isEqualTo("404"),
          finding -> assertThat(finding.parameterName()).isNull(),
          finding -> assertThat(finding.contentType()).isNull()
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_030_UNJUDGED_TARGET_IS_NOT_APPLICABLE)
    void shouldJudgeEveryDocumentedEntryWithNoneInapplicable() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200", "404", "500"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        new HashMap<>()
      );

      assertThat(result)
        .extracting(ApiTestFinding::responseCode, ApiTestFinding::status)
        .containsExactly(
          tuple("200", UNCOVERED),
          tuple("404", UNCOVERED),
          tuple("500", UNCOVERED)
        );
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
    void shouldEvidenceOnlyTheSpansWhoseStatusCodeMatchedTheEntry() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200", "404"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          createTelemetryDataWithStatusCode("200", "okTraceId"),
          createTelemetryDataWithStatusCode("404", "notFoundTraceId")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "404".equals(finding.responseCode()))
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(COVERED),
          finding ->
            assertThat(finding.evidence()).containsExactly(
              new FindingEvidence("notFoundTraceId", null)
            )
        );
    }

    @Test
    void shouldCarryNoEvidenceOnAnUncoveredFinding() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200", "500"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(createTelemetryDataWithStatusCode("200", "okTraceId"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "500".equals(finding.responseCode()))
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(UNCOVERED),
          finding -> assertThat(finding.evidence()).isEmpty()
        );
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_002_RESPONSE_CODE_COVERAGE_TREATS_DEFAULT_AS_WILDCARD
    )
    void shouldEvidenceTheDefaultEntryWithExactlyTheSpansNoSiblingMatched() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200", "4XX", "default"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          createTelemetryDataWithStatusCode("200", "okTraceId"),
          createTelemetryDataWithStatusCode("404", "notFoundTraceId"),
          createTelemetryDataWithStatusCode("500", "serverErrorTraceId"),
          createTelemetryDataWithStatusCode("503", "unavailableTraceId")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "default".equals(finding.responseCode()))
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(COVERED),
          finding ->
            assertThat(finding.evidence()).containsExactly(
              new FindingEvidence("serverErrorTraceId", null),
              new FindingEvidence("unavailableTraceId", null)
            )
        );
    }

    @Test
    @VerifiesSw(
      SwTraceables.SW_002_RESPONSE_CODE_COVERAGE_TREATS_DEFAULT_AS_WILDCARD
    )
    void shouldLeaveTheDefaultEntryUnevidencedWhenEverySpanMatchedASibling() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200", "default"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(createTelemetryDataWithStatusCode("200", "okTraceId"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "default".equals(finding.responseCode()))
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(UNCOVERED),
          finding -> assertThat(finding.evidence()).isEmpty()
        );
    }

    @Test
    void shouldCollapseSpansOfOneTraceIntoASingleEvidenceEntry() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          createTelemetryDataWithStatusCode("200", "traceId"),
          createTelemetryDataWithStatusCode("200", "traceId")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .extracting(ApiTestFinding::evidence, as(list(FindingEvidence.class)))
        .containsExactly(new FindingEvidence("traceId", null));
    }

    private Map<String, Operation> createOperationsWithCodes(
      Map<String, List<String>> operationKeyToCodes
    ) {
      Map<String, Operation> result = new HashMap<>();

      for (Map.Entry<
        String,
        List<String>
      > entry : operationKeyToCodes.entrySet()) {
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

    private OpenTelemetryData createTelemetryDataWithStatusCode(
      String statusCode,
      String traceId
    ) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("http.response.status_code", statusCode);

      return new OpenTelemetryData("span-" + traceId, traceId, attributes);
    }
  }
}

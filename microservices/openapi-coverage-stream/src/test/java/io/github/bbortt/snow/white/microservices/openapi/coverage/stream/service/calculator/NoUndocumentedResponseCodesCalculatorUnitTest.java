/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.NO_UNDOCUMENTED_RESPONSE_CODES;
import static java.math.RoundingMode.HALF_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;

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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith({ MockitoExtension.class })
class NoUndocumentedResponseCodesCalculatorUnitTest {

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
      OpenApiCoverageCriteria openApiCriteria
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
    @VerifiesSw(SwTraceables.SW_003_UNDOCUMENTED_RESPONSE_CODE_DETECTION)
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

    /**
     * A status code observed under two concrete paths of one templated operation is one question
     * about that operation, so it enters the fraction once.
     * Counting it per request path — as this criterion did while telemetry was grouped by the
     * key it arrived under — would put the same code in the denominator twice and read 0.67 here.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_003_UNDOCUMENTED_RESPONSE_CODE_DETECTION)
    void shouldCountAnObservedCodeOnce_whenSeveralConcretePathsShareOneOperation() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/pung/{message}", List.of("200"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of(
          "GET_/pung/hello",
          List.of("200"),
          "GET_/pung/world",
          List.of("200", "503")
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes are not documented in the OpenAPI specification: `GET_/pung/{message} [503]`"
          )
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

    @Test
    void shouldSkipOperation_whenTelemetryListIsEmpty() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of())
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
    void shouldTreatObservedCodesAsUndocumented_whenNoOperationMatchesPath() {
      var pathToOpenAPIOperationMap = new HashMap<String, Operation>();

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/unknown", List.of("200"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following response codes are not documented in the OpenAPI specification: `GET_/api/v1/unknown [200]`"
          )
      );
    }

    @Test
    void shouldIgnoreTelemetryWithNullAttributes() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(new OpenTelemetryData("span-123", "trace-456", null))
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
    void shouldHandleUppercaseDefaultCatchAll() {
      var pathToOpenAPIOperationMap = createOperationsWithResponseCodes(
        Map.of("GET_/api/v1/users", List.of("200", "DEFAULT"))
      );

      var pathToTelemetryMap = createTelemetryWithStatusCodes(
        Map.of("GET_/api/v1/users", List.of("200", "500"))
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

  @Nested
  class CalculateFindingsTest {

    private static final String OPENAPI_DOCUMENT = """
    {
      "paths": {
        "/pung/{message}": {
          "get": { "responses": { "200": { "description": "ok" } } }
        },
        "/responseless": {
          "get": { "operationId": "responseless" }
        }
      }
    }
    """;

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldNameTheResponsesContainerAndCarryTheJudgedCodeBesideIt() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/pung/{message}", List.of("200"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/pung/hello",
        List.of(createTelemetryDataWithStatusCode("404", "notFoundTraceId"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding ->
            assertThat(finding.specPointer()).isEqualTo(
              "/paths/~1pung~1{message}/get/responses"
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
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldTellTheFindingsOfOneOperationApartByResponseCodeAlone() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          createTelemetryDataWithStatusCode("200", "okTraceId"),
          createTelemetryDataWithStatusCode("503", "unavailableTraceId")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .hasSize(2)
        .allSatisfy(finding ->
          assertThat(finding.specPointer()).isEqualTo(
            "/paths/~1api~1v1~1users/get/responses"
          )
        )
        .extracting(ApiTestFinding::responseCode, ApiTestFinding::status)
        .containsExactly(tuple("200", COVERED), tuple("503", UNCOVERED));
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
    void shouldEvidenceTheUncoveredFindingWithTheTracesThatExhibitedTheCode() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          createTelemetryDataWithStatusCode("200", "okTraceId"),
          createTelemetryDataWithStatusCode("503", "unavailableTraceId")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "503".equals(finding.responseCode()))
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(UNCOVERED),
          finding ->
            assertThat(finding.evidence()).containsExactly(
              new FindingEvidence("unavailableTraceId", null)
            )
        );
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
    void shouldEvidenceACoveredFindingWithItsOwnTracesOnly() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          createTelemetryDataWithStatusCode("200", "okTraceId"),
          createTelemetryDataWithStatusCode("503", "unavailableTraceId")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "200".equals(finding.responseCode()))
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.status()).isEqualTo(COVERED),
          finding ->
            assertThat(finding.evidence()).containsExactly(
              new FindingEvidence("okTraceId", null)
            )
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_003_UNDOCUMENTED_RESPONSE_CODE_DETECTION)
    void shouldAskOneQuestionPerOperation_whenOneCodeIsObservedUnderSeveralConcretePaths() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/pung/{message}", List.of("200"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/pung/hello",
        List.of(createTelemetryDataWithStatusCode("200", "helloTraceId")),
        "GET_/pung/world",
        List.of(createTelemetryDataWithStatusCode("200", "worldTraceId"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding ->
            assertThat(finding.httpPath()).isEqualTo("/pung/{message}"),
          finding -> assertThat(finding.responseCode()).isEqualTo("200"),
          finding ->
            assertThat(finding.evidence()).containsExactly(
              new FindingEvidence("helloTraceId", null),
              new FindingEvidence("worldTraceId", null)
            )
        );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldPointAtTheOperation_whenItDocumentsNoResponses() {
      var operation = new Operation();
      operation.setResponses(null);

      var pathToTelemetryMap = Map.of(
        "GET_/responseless",
        List.of(createTelemetryDataWithStatusCode("200", "okTraceId"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        Map.of("GET_/responseless", operation),
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .extracting(ApiTestFinding::specPointer)
        .isEqualTo("/paths/~1responseless/get");
    }

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldPointAtThePathsMap_whenTheDocumentDescribesTheOperationNowhere() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/pung/{message}", List.of("200"))
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/unknown",
        List.of(createTelemetryDataWithStatusCode("200", "okTraceId"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.specPointer()).isEqualTo("/paths"),
          finding -> assertThat(finding.status()).isEqualTo(UNCOVERED),
          finding -> assertThat(finding.httpPath()).isEqualTo("/api/v1/unknown")
        );
    }

    /**
     * The pointer has to resolve on every rung of the ladder — that is what would fail if one
     * were ever built toward a response entry the document does not contain.
     */
    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldResolveEveryPointerAgainstTheDocument() {
      var responseless = new Operation();
      responseless.setResponses(null);

      var pathToOpenAPIOperationMap = new HashMap<>(
        createOperationsWithCodes(Map.of("GET_/pung/{message}", List.of("200")))
      );
      pathToOpenAPIOperationMap.put("GET_/responseless", responseless);

      var pathToTelemetryMap = Map.of(
        "GET_/pung/hello",
        List.of(createTelemetryDataWithStatusCode("418", "teapotTraceId")),
        "GET_/responseless",
        List.of(createTelemetryDataWithStatusCode("418", "teapotTraceId")),
        "GET_/api/v1/unknown",
        List.of(createTelemetryDataWithStatusCode("418", "teapotTraceId"))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      JsonNode document = JsonMapper.shared().readTree(OPENAPI_DOCUMENT);

      assertThat(result)
        .hasSize(3)
        .allSatisfy(finding ->
          assertThat(document.at(finding.specPointer()).isMissingNode())
            .as(finding.specPointer())
            .isFalse()
        );
    }

    @Test
    void shouldRecordNoFinding_whenNoSpanCarriesAStatusCode() {
      var pathToOpenAPIOperationMap = createOperationsWithCodes(
        Map.of("GET_/api/v1/users", List.of("200"))
      );

      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("some.other.attribute", "value");

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(new OpenTelemetryData("span-123", "traceId", attributes))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).isEmpty();
    }

    private Map<String, Operation> createOperationsWithCodes(
      Map<String, List<String>> operationKeyToCodes
    ) {
      Map<String, Operation> operations = new HashMap<>();

      operationKeyToCodes.forEach((operationKey, codes) -> {
        var responses = new ApiResponses();
        codes.forEach(code ->
          responses.addApiResponse(code, new ApiResponse().description("Test"))
        );

        var operation = new Operation();
        operation.setResponses(responses);
        operations.put(operationKey, operation);
      });

      return operations;
    }

    private OpenTelemetryData createTelemetryDataWithStatusCode(
      String statusCode,
      String traceId
    ) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("http.response.status_code", statusCode);

      return new OpenTelemetryData("span-123", traceId, attributes);
    }
  }
}

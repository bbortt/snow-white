/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.CONTENT_TYPE_COVERAGE;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator.ContentTypeCoverageCalculator.CONTENT_TYPE_HEADER_KEY;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.FindingStatus.COVERED;
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
import io.swagger.v3.oas.models.parameters.RequestBody;
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
class ContentTypeCoverageCalculatorUnitTest {

  private ContentTypeCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ContentTypeCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenContentTypeCoverage() {
      boolean result = fixture.accepts(CONTENT_TYPE_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotContentTypeCoverage(
      OpenApiCoverageCriteria openApiCriteria
    ) {
      if (CONTENT_TYPE_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllContentTypesCovered() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json", "multipart/form-data")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(
          telemetryWithContentType("application/json"),
          telemetryWithContentType("multipart/form-data")
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(CONTENT_TYPE_COVERAGE),
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
    void shouldReturn50Percent_whenSomeContentTypesCovered() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/documents",
        operationWithContentTypes("application/json", "multipart/form-data")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/documents",
        List.of(telemetryWithContentType("application/json"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(CONTENT_TYPE_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following request body content types are uncovered: `POST_/api/v1/documents [multipart/form-data]`"
          )
      );
    }

    @Test
    void shouldReturn0Percent_whenNoContentTypesCovered() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(CONTENT_TYPE_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following request body content types are uncovered: `POST_/api/v1/users [application/json]`"
          )
      );
    }

    @Test
    @VerifiesSw(SwTraceables.SW_005_CONTENT_TYPE_COVERAGE)
    void shouldSkipOperationsWithNoRequestBody() {
      var operationWithoutBody = new Operation();

      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        operationWithoutBody
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

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
    void shouldMatchContentTypeWithCharsetSuffix() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      // Observed value includes charset parameter — should still match
      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(telemetryWithContentType("application/json; charset=utf-8"))
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
    void shouldHandleContentTypeAsJsonArray() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      var attributes = JsonMapper.shared().createObjectNode();
      var arrayNode = attributes.putArray(CONTENT_TYPE_HEADER_KEY);
      arrayNode.add("application/json");

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(new OpenTelemetryData("span-1", "trace-1", attributes))
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
    @VerifiesSw(SwTraceables.SW_005_CONTENT_TYPE_COVERAGE)
    void shouldSkipTelemetryWithNullAttributes() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(new OpenTelemetryData("span-1", "trace-1", null))
      );

      // Telemetry exists but carries no header at all — this is exactly the case the header-capture
      // hint (verified separately below) must catch, so the plain uncovered-list message alone is
      // no longer the right expectation.

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            """
            The following request body content types are uncovered: `POST_/api/v1/users [application/json]`
            No `content-type` header was observed on any correlated telemetry — header capture may not be enabled (see `OTEL_INSTRUMENTATION_HTTP_SERVER_CAPTURE_REQUEST_HEADERS`, pages/_pages/onboarding.md)."""
          )
      );
    }

    @Test
    void shouldHandleMultipleOperations() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json"),
        "PUT_/api/v1/users/{id}",
        operationWithContentTypes("application/json")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(telemetryWithContentType("application/json")),
        "PUT_/api/v1/users/42",
        List.of(telemetryWithContentType("application/json"))
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

    private Operation operationWithContentTypes(String... contentTypes) {
      var content = new Content();
      for (String contentType : contentTypes) {
        content.addMediaType(contentType, new MediaType());
      }

      var requestBody = new RequestBody();
      requestBody.setContent(content);

      var operation = new Operation();
      operation.setRequestBody(requestBody);
      return operation;
    }

    private OpenTelemetryData telemetryWithContentType(String contentType) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put(CONTENT_TYPE_HEADER_KEY, contentType);
      return new OpenTelemetryData("span-1", "trace-1", attributes);
    }

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, HALF_UP);
    }
  }

  @Nested
  class CalculateFindingsTest {

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldReturnOneFindingPerDeclaredMediaTypeOrderedByOperationKey() {
      var pathToOpenAPIOperationMap = Map.of(
        "PUT_/api/v1/users/{id}",
        operationWithContentTypes("application/json"),
        "POST_/api/v1/users",
        operationWithContentTypes("application/json", "application/xml")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(telemetry("trace-json", "application/json"))
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
          ApiTestFinding::contentType
        )
        .containsExactly(
          tuple(
            "/paths/~1api~1v1~1users/post/requestBody/content/application~1json",
            COVERED,
            "/api/v1/users",
            "POST",
            "application/json"
          ),
          tuple(
            "/paths/~1api~1v1~1users/post/requestBody/content/application~1xml",
            UNCOVERED,
            "/api/v1/users",
            "POST",
            "application/xml"
          ),
          tuple(
            "/paths/~1api~1v1~1users~1{id}/put/requestBody/content/application~1json",
            UNCOVERED,
            "/api/v1/users/{id}",
            "PUT",
            "application/json"
          )
        );
    }

    @Test
    void shouldContributeNoTargetForAnOperationWithoutARequestBody() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        new Operation(),
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .singleElement()
        .extracting(ApiTestFinding::httpMethod)
        .isEqualTo("POST");
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
    void shouldEvidenceOnlyTheSpansThatSentTheDeclaredMediaType() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json", "application/xml")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(
          telemetry("trace-json", "application/json"),
          telemetry("trace-xml", "application/xml")
        )
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "application/xml".equals(finding.contentType()))
        .singleElement()
        .extracting(ApiTestFinding::evidence, as(list(FindingEvidence.class)))
        .containsExactly(new FindingEvidence("trace-xml", null));
    }

    @Test
    @VerifiesSw(SwTraceables.SW_005_CONTENT_TYPE_COVERAGE)
    void shouldEvidenceADeclaredMediaTypeCarryingAdditionalParameters() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(telemetry("trace-json", "application/json;charset=UTF-8"))
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
              new FindingEvidence("trace-json", null)
            )
        );
    }

    @Test
    void shouldCarryNoEvidenceOnAnUncoveredFinding() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/users",
        List.of(telemetry("trace-xml", "application/xml"))
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
    void shouldLeaveTheResponseCodeAndParameterNameUnset() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        operationWithContentTypes("application/json")
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.responseCode()).isNull(),
          finding -> assertThat(finding.parameterName()).isNull()
        );
    }

    private Operation operationWithContentTypes(String... contentTypes) {
      var content = new Content();
      for (String contentType : contentTypes) {
        content.addMediaType(contentType, new MediaType());
      }

      var requestBody = new RequestBody();
      requestBody.setContent(content);

      var operation = new Operation();
      operation.setRequestBody(requestBody);
      return operation;
    }

    private OpenTelemetryData telemetry(String traceId, String contentType) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put(CONTENT_TYPE_HEADER_KEY, contentType);
      return new OpenTelemetryData("span-1", traceId, attributes);
    }
  }
}

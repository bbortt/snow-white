/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.OPERATION_SUCCESS_COVERAGE;
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
class OperationSuccessCoverageCalculatorUnitTest {

  private OperationSuccessCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OperationSuccessCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenOperationSuccessCoverage() {
      boolean result = fixture.accepts(OPERATION_SUCCESS_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotOperationSuccessCoverage(
      OpenApiCoverageCriteria openApiCriteria
    ) {
      if (OPERATION_SUCCESS_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllOperationsHaveSuccessfulResponse() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        new Operation(),
        "POST_/api/v1/users",
        new Operation()
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetryWithStatusCode("200")),
        "POST_/api/v1/users",
        List.of(telemetryWithStatusCode("201"))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(OPERATION_SUCCESS_COVERAGE),
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
    void shouldReturn50Percent_whenOnlySomeOperationsHaveSuccessfulResponse() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        new Operation(),
        "DELETE_/api/v1/users/{id}",
        new Operation()
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetryWithStatusCode("200")),
        "DELETE_/api/v1/users/{id}",
        List.of(telemetryWithStatusCode("404")) // called but never succeeded
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(OPERATION_SUCCESS_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following operations have no successful (2xx) response observed: `DELETE_/api/v1/users/{id}`"
          )
      );
    }

    @Test
    void shouldReturn0Percent_whenNoOperationsHaveSuccessfulResponse() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        new Operation(),
        "POST_/api/v1/users",
        new Operation()
      );

      var pathToTelemetryMap = new HashMap<String, List<OpenTelemetryData>>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r ->
          assertThat(r.openApiCriteria()).isEqualTo(OPERATION_SUCCESS_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r -> assertThat(r.additionalInformation()).isNotNull()
      );
    }

    @Test
    void shouldCountOperationAsSuccessful_whenMixedResponsesInclude2xx() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/orders",
        new Operation()
      );

      // Same operation: some calls fail, one succeeds
      var pathToTelemetryMap = Map.of(
        "POST_/api/v1/orders",
        List.of(
          telemetryWithStatusCode("400"),
          telemetryWithStatusCode("500"),
          telemetryWithStatusCode("201")
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

    @Test
    @VerifiesSw(SwTraceables.SW_001_STRUCTURAL_CALL_COVERAGE)
    void shouldNotCountErrorOnlyResponses_asSuccessful() {
      var pathToOpenAPIOperationMap = Map.of(
        "DELETE_/api/v1/users/{id}",
        new Operation()
      );

      var pathToTelemetryMap = Map.of(
        "DELETE_/api/v1/users/{id}",
        List.of(
          telemetryWithStatusCode("400"),
          telemetryWithStatusCode("403"),
          telemetryWithStatusCode("500")
        )
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following operations have no successful (2xx) response observed: `DELETE_/api/v1/users/{id}`"
          )
      );
    }

    @Test
    void shouldMatchTelemetryViaPathTemplate() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users/{id}",
        new Operation()
      );

      // Telemetry uses the concrete resolved path, not the template
      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users/42",
        List.of(telemetryWithStatusCode("200"))
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
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        new Operation()
      );

      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("some.other.attribute", "value");

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(new OpenTelemetryData("span-1", "trace-1", attributes))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r -> assertThat(r.additionalInformation()).isNotNull()
      );
    }

    private OpenTelemetryData telemetryWithStatusCode(String statusCode) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("http.response.status_code", statusCode);
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
    void shouldReturnOneFindingPerOperationOrderedByOperationKey() {
      var pathToOpenAPIOperationMap = Map.of(
        "POST_/api/v1/users",
        new Operation(),
        "GET_/api/v1/users",
        new Operation()
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetry("trace-ok", "200"))
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
          ApiTestFinding::httpMethod
        )
        .containsExactly(
          tuple("/paths/~1api~1v1~1users/get", COVERED, "/api/v1/users", "GET"),
          tuple(
            "/paths/~1api~1v1~1users/post",
            UNCOVERED,
            "/api/v1/users",
            "POST"
          )
        );
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
    void shouldEvidenceEverySuccessfulSpanAndNoOther() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        new Operation()
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(
          telemetry("trace-created", "201"),
          telemetry("trace-failed", "500"),
          telemetry("trace-ok", "200")
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
          new FindingEvidence("trace-created", null),
          new FindingEvidence("trace-ok", null)
        );
    }

    @Test
    void shouldCarryNoEvidenceWhenOnlyUnsuccessfulSpansWereObserved() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        new Operation()
      );

      var pathToTelemetryMap = Map.of(
        "GET_/api/v1/users",
        List.of(telemetry("trace-failed", "500"))
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
    void shouldLeaveEveryDiscriminatorButThePathAndMethodUnset() {
      var pathToOpenAPIOperationMap = Map.of(
        "GET_/api/v1/users",
        new Operation()
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        Map.of()
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.responseCode()).isNull(),
          finding -> assertThat(finding.parameterName()).isNull(),
          finding -> assertThat(finding.contentType()).isNull()
        );
    }

    private OpenTelemetryData telemetry(String traceId, String statusCode) {
      var attributes = JsonMapper.shared().createObjectNode();
      attributes.put("http.response.status_code", statusCode);
      return new OpenTelemetryData("span-1", traceId, attributes);
    }
  }
}

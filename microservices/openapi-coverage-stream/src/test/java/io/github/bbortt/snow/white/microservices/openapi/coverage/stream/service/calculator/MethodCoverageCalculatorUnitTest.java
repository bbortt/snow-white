/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.COVERED;
import static io.github.bbortt.snow.white.commons.event.dto.FindingStatus.UNCOVERED;
import static io.github.bbortt.snow.white.commons.quality.gate.OpenApiCoverageCriteria.HTTP_METHOD_COVERAGE;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.InstanceOfAssertFactories.INTEGER;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.Mockito.mock;

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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class MethodCoverageCalculatorUnitTest {

  @Mock
  private Operation operationMock;

  private MethodCoverageCalculator fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new MethodCoverageCalculator();
  }

  @Nested
  class AcceptsTest {

    @Test
    void shouldReturnTrue_whenPathCoverage() {
      boolean result = fixture.accepts(HTTP_METHOD_COVERAGE);

      assertThat(result).isTrue();
    }

    @EnumSource
    @ParameterizedTest
    void shouldReturnFalse_whenNotPathCoverage(
      OpenApiCoverageCriteria openApiCriteria
    ) {
      if (HTTP_METHOD_COVERAGE.equals(openApiCriteria)) {
        return;
      }

      boolean result = fixture.accepts(openApiCriteria);

      assertThat(result).isFalse();
    }
  }

  @Nested
  class CalculatesTest {

    @Test
    void shouldReturn100Percent_whenAllPathsAndMethodsCovered() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("POST_/api/v1/comments", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(mock(OpenTelemetryData.class))
      );
      pathToTelemetryMap.put(
        "POST_/api/v1/comments",
        singletonList(mock(OpenTelemetryData.class))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(HTTP_METHOD_COVERAGE),
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
    @VerifiesSw(SwTraceables.SW_001_STRUCTURAL_CALL_COVERAGE)
    void shouldReturn50Percent_whenHalfPathsCovered() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("GET_/api/v1/comments", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(mock(OpenTelemetryData.class))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(HTTP_METHOD_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following paths are uncovered: `GET_/api/v1/comments`"
          )
      );
    }

    @Test
    void shouldReturn50Percent_whenHalfMethodsPathsCovered() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("POST_/api/v1/users", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(mock(OpenTelemetryData.class))
      );

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(HTTP_METHOD_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.5)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following paths are uncovered: `POST_/api/v1/users`"
          )
      );
    }

    @Test
    void shouldReturn0Percent_whenNoPathsNorMethodsCovered() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("POST_/api/v1/users", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();

      OpenApiTestResult result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(HTTP_METHOD_COVERAGE),
        r -> assertThat(r.coverage()).isEqualTo(getBigDecimal(0.0)),
        r ->
          assertThat(r.duration())
            .isNotNull()
            .extracting(Duration::getNano)
            .asInstanceOf(INTEGER)
            .isPositive(),
        r ->
          assertThat(r.additionalInformation()).isEqualTo(
            "The following paths are uncovered: `GET_/api/v1/users`, `POST_/api/v1/users`"
          )
      );
    }

    @Test
    void shouldReturn100Percent_whenTelemetryUsesConcretePathForTemplate() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/pung/{message}", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/pung/hello",
        singletonList(mock(OpenTelemetryData.class))
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
    void shouldHandleEmptyOperationsMap() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(mock(OpenTelemetryData.class))
      );

      var result = fixture.calculate(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result).satisfies(
        r -> assertThat(r.openApiCriteria()).isEqualTo(HTTP_METHOD_COVERAGE),
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

    private static @NonNull BigDecimal getBigDecimal(double value) {
      return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
  }

  @Nested
  class CalculateFindingsTest {

    @Test
    @VerifiesSw(SwTraceables.SW_029_FINDING_IDENTIFIED_BY_SPEC_POINTER)
    void shouldReturnOneFindingPerOperationOrderedByOperationKey() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("POST_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("GET_/api/v1/orders", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(new OpenTelemetryData("spanId", "traceId", null))
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
          tuple(
            "/paths/~1api~1v1~1orders/get",
            UNCOVERED,
            "/api/v1/orders",
            "GET"
          ),
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
    void shouldLeaveEveryDiscriminatorButThePathAndMethodUnset() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        new HashMap<>()
      );

      assertThat(result)
        .singleElement()
        .satisfies(
          finding -> assertThat(finding.responseCode()).isNull(),
          finding -> assertThat(finding.parameterName()).isNull(),
          finding -> assertThat(finding.contentType()).isNull()
        );
    }

    @Test
    @VerifiesArch(ArchTraceables.ARCH_011_EVIDENCE_CAPTURED_AT_THE_MATCH)
    void shouldEvidenceOnlyTheSpansObservedOnTheFindingsOwnOperation() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);
      pathToOpenAPIOperationMap.put("POST_/api/v1/users", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/api/v1/users",
        singletonList(new OpenTelemetryData("spanId1", "getTraceId", null))
      );
      pathToTelemetryMap.put(
        "POST_/api/v1/users",
        singletonList(new OpenTelemetryData("spanId2", "postTraceId", null))
      );

      List<ApiTestFinding> result = fixture.calculateFindings(
        pathToOpenAPIOperationMap,
        pathToTelemetryMap
      );

      assertThat(result)
        .filteredOn(finding -> "POST".equals(finding.httpMethod()))
        .singleElement()
        .extracting(ApiTestFinding::evidence, as(list(FindingEvidence.class)))
        .containsExactly(new FindingEvidence("postTraceId", null));
    }

    @Test
    void shouldEvidenceATemplatedOperationFromTelemetryOnItsConcretePath() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/pung/{message}", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "GET_/pung/hello",
        singletonList(new OpenTelemetryData("spanId", "traceId", null))
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
              new FindingEvidence("traceId", null)
            )
        );
    }

    @Test
    void shouldCarryNoEvidenceOnAnUncoveredFinding() {
      Map<String, Operation> pathToOpenAPIOperationMap = new HashMap<>();
      pathToOpenAPIOperationMap.put("GET_/api/v1/users", operationMock);

      Map<String, List<OpenTelemetryData>> pathToTelemetryMap = new HashMap<>();
      pathToTelemetryMap.put(
        "POST_/api/v1/users",
        singletonList(new OpenTelemetryData("spanId", "traceId", null))
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
  }
}

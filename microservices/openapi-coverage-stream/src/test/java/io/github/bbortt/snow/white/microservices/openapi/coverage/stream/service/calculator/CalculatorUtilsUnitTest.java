/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.commons.event.dto.FindingEvidence;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculatorUtilsUnitTest {

  @Nested
  class getStartedStopWatchTest {

    @Test
    void shouldReturnStartedStopWatch() {
      var startedStopWatch = CalculatorUtils.getStartedStopWatch();

      assertThat(startedStopWatch)
        .isNotNull()
        .extracting(StopWatch::isStarted)
        .isEqualTo(true);
    }
  }

  @Nested
  class GetTelemetryForTemplateTest {

    @Test
    void shouldReturnTelemetryForExactMatch() {
      var telemetry = mock(OpenTelemetryData.class);
      var telemetryMap = Map.of("GET_/ping", List.of(telemetry));

      var result = CalculatorUtils.getTelemetryForTemplate(
        telemetryMap,
        "GET_/ping"
      );

      assertThat(result).containsExactly(telemetry);
    }

    @Test
    void shouldReturnTelemetryWhenConcretePathMatchesTemplate() {
      var telemetry = mock(OpenTelemetryData.class);
      var telemetryMap = Map.of("GET_/pung/hello", List.of(telemetry));

      var result = CalculatorUtils.getTelemetryForTemplate(
        telemetryMap,
        "GET_/pung/{message}"
      );

      assertThat(result).containsExactly(telemetry);
    }

    @Test
    void shouldReturnEmptyListWhenNoMatch() {
      var telemetryMap = Map.of(
        "GET_/other",
        List.of(mock(OpenTelemetryData.class))
      );

      var result = CalculatorUtils.getTelemetryForTemplate(
        telemetryMap,
        "GET_/pung/{message}"
      );

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class GetTelemetryForPathTemplateTest {

    @Test
    void shouldReturnTelemetryOfEveryMethodOnTheMatchingPath() {
      var getTelemetry = mock(OpenTelemetryData.class);
      var postTelemetry = mock(OpenTelemetryData.class);
      var telemetryMap = Map.of(
        "GET_/pung/hello",
        List.of(getTelemetry),
        "POST_/pung/world",
        List.of(postTelemetry)
      );

      var result = CalculatorUtils.getTelemetryForPathTemplate(
        telemetryMap,
        "/pung/{message}"
      );

      assertThat(result).containsExactlyInAnyOrder(getTelemetry, postTelemetry);
    }

    @Test
    void shouldReturnEmptyListWhenNoPathMatches() {
      var telemetryMap = Map.of(
        "GET_/other",
        List.of(mock(OpenTelemetryData.class))
      );

      var result = CalculatorUtils.getTelemetryForPathTemplate(
        telemetryMap,
        "/pung/{message}"
      );

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class ToEvidenceTest {

    @Test
    void shouldReturnOneEntryPerDistinctTraceWithoutTestCaseName() {
      var result = CalculatorUtils.toEvidence(
        List.of(
          new OpenTelemetryData("spanId1", "traceId1", null),
          new OpenTelemetryData("spanId2", "traceId1", null),
          new OpenTelemetryData("spanId3", "traceId2", null)
        )
      );

      assertThat(result).containsExactly(
        new FindingEvidence("traceId1", null),
        new FindingEvidence("traceId2", null)
      );
    }

    @Test
    void shouldReturnEmptyListWhenNothingSatisfiedTheTarget() {
      var result = CalculatorUtils.toEvidence(emptyList());

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class FindOperationForConcreteKeyTest {

    @Test
    void shouldReturnOperationForExactMatch() {
      var operation = mock(Operation.class);
      var operationMap = Map.of("GET_/ping", operation);

      var result = CalculatorUtils.findOperationForConcreteKey(
        operationMap,
        "GET_/ping"
      );

      assertThat(result).isSameAs(operation);
    }

    @Test
    void shouldReturnOperationWhenConcretePathMatchesTemplate() {
      var operation = mock(Operation.class);
      var operationMap = Map.of("GET_/pung/{message}", operation);

      var result = CalculatorUtils.findOperationForConcreteKey(
        operationMap,
        "GET_/pung/hello"
      );

      assertThat(result).isSameAs(operation);
    }

    @Test
    void shouldReturnNullWhenNoMatch() {
      var operationMap = Map.of("GET_/ping", mock(Operation.class));

      var result = CalculatorUtils.findOperationForConcreteKey(
        operationMap,
        "GET_/pung/hello"
      );

      assertThat(result).isNull();
    }
  }

  @Nested
  class FindOperationEntryForConcreteKeyTest {

    @Test
    void shouldReturnTheExactKeyItMatched() {
      var operation = mock(Operation.class);
      var operationMap = Map.of("GET_/ping", operation);

      var result = CalculatorUtils.findOperationEntryForConcreteKey(
        operationMap,
        "GET_/ping"
      );

      assertThat(result)
        .isNotNull()
        .satisfies(
          entry -> assertThat(entry.getKey()).isEqualTo("GET_/ping"),
          entry -> assertThat(entry.getValue()).isSameAs(operation)
        );
    }

    /**
     * The template key, not the concrete one the caller asked with — a finding has to name its
     * target the way the document spells it.
     */
    @Test
    void shouldReturnTheTemplateKeyWhenAConcretePathMatchedIt() {
      var operation = mock(Operation.class);
      var operationMap = Map.of("GET_/pung/{message}", operation);

      var result = CalculatorUtils.findOperationEntryForConcreteKey(
        operationMap,
        "GET_/pung/hello"
      );

      assertThat(result)
        .isNotNull()
        .satisfies(
          entry -> assertThat(entry.getKey()).isEqualTo("GET_/pung/{message}"),
          entry -> assertThat(entry.getValue()).isSameAs(operation)
        );
    }

    @Test
    void shouldReturnNullWhenNoMatch() {
      var operationMap = Map.of("GET_/ping", mock(Operation.class));

      var result = CalculatorUtils.findOperationEntryForConcreteKey(
        operationMap,
        "GET_/pung/hello"
      );

      assertThat(result).isNull();
    }
  }
}

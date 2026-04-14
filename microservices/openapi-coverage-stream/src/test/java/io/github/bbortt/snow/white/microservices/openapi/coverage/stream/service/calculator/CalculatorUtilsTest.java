/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.dto.OpenTelemetryData;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CalculatorUtilsTest {

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
}

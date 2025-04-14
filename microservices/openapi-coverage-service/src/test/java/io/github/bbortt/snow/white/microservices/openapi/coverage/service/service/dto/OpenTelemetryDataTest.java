/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.influxdb.query.FluxRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenTelemetryDataTest {

  @Nested
  class ParseOpenTelemetryData {

    @Mock
    private FluxRecord mockRecord;

    @Test
    void withValidJsonAttributes() {
      when(mockRecord.getValueByKey(OpenTelemetryData.SPAN_ID_KEY)).thenReturn(
        "00f067aa0ba902b7"
      );
      when(mockRecord.getValueByKey(OpenTelemetryData.TRACE_ID_KEY)).thenReturn(
        "4bf92f3577b34da6a3ce929d0e0e4736"
      );
      when(mockRecord.getValueByKey(OpenTelemetryData.VALUE_KEY)).thenReturn(
        "{\"key\":\"value\"}"
      );

      OpenTelemetryData result = OpenTelemetryData.parseOpenTelemetryData(
        mockRecord
      );

      assertThat(result).satisfies(
        r -> assertThat(r.spanId()).isEqualTo("00f067aa0ba902b7"),
        r ->
          assertThat(r.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736")
      );

      var attributes = result.attributes();
      assertThat(attributes).isNotNull();
      assertThat(attributes.get("key").asText()).isEqualTo("value");
    }

    @Test
    void withInvalidJsonAttributes() {
      when(mockRecord.getValueByKey(OpenTelemetryData.SPAN_ID_KEY)).thenReturn(
        "9f9c8d2c3a1e4bcd"
      );
      when(mockRecord.getValueByKey(OpenTelemetryData.TRACE_ID_KEY)).thenReturn(
        "d4cda95b652f4a1592b449d5929fda1b"
      );
      when(mockRecord.getValueByKey(OpenTelemetryData.VALUE_KEY)).thenReturn(
        "not-a-json"
      );

      OpenTelemetryData result = OpenTelemetryData.parseOpenTelemetryData(
        mockRecord
      );

      assertThat(result).satisfies(
        r -> assertThat(r.spanId()).isEqualTo("9f9c8d2c3a1e4bcd"),
        r ->
          assertThat(r.traceId()).isEqualTo("d4cda95b652f4a1592b449d5929fda1b")
      );

      var attributes = result.attributes();
      assertThat(attributes).isInstanceOf(ObjectNode.class).isEmpty();
    }
  }
}

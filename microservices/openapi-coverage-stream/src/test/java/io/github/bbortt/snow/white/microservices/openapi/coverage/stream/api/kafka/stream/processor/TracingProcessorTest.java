/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.stream.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.captor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class TracingProcessorTest {

  @Mock
  private ContextPropagators propagatorsMock;

  @Mock
  private FixedKeyProcessorContext<String, String> contextMock;

  @Mock
  private TextMapPropagator textMapPropagatorMock;

  @Test
  void shouldPropagateTracingContext() {
    try (var globalOpenTelemetry = mockStatic(GlobalOpenTelemetry.class)) {
      doReturn(textMapPropagatorMock)
        .when(propagatorsMock)
        .getTextMapPropagator();
      globalOpenTelemetry
        .when(GlobalOpenTelemetry::getPropagators)
        .thenReturn(propagatorsMock);

      var inputHeaders = new RecordHeaders();
      FixedKeyRecord<String, String> inputRecord = mock();
      doReturn(inputHeaders).when(inputRecord).headers();

      FixedKeyRecord<String, String> outgoingRecord = mock();
      doReturn(inputHeaders).when(outgoingRecord).headers();
      doReturn("processedValue").when(outgoingRecord).value();
      doReturn(outgoingRecord).when(inputRecord).withValue("processedValue");

      doReturn(Context.root())
        .when(textMapPropagatorMock)
        .extract(any(), any(Headers.class), any());

      var fixture = new TracingProcessor<String, String, String>(
        (recordUnderTest, _) -> {
          assertThat(recordUnderTest).isEqualTo(inputRecord);
          return "processedValue";
        }
      );
      fixture.init(contextMock);

      fixture.process(inputRecord);

      verify(textMapPropagatorMock).extract(any(), any(Headers.class), any());
      verify(textMapPropagatorMock).inject(any(), any(), any());

      ArgumentCaptor<FixedKeyRecord<String, String>> outgoingRecordCaptor =
        captor();
      verify(contextMock).forward(outgoingRecordCaptor.capture());
      assertThat(outgoingRecordCaptor.getValue().value()).isEqualTo(
        "processedValue"
      );
    }
  }
}

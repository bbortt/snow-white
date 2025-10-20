/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka.serialization;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.TestData.qualityGateCalculationRequestEvent;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QualityGateCalculationEventSerdesTest {

  @Nested
  class QualityGateCalculationRequestEvent {

    @Test
    void serializationAndDeserializationLoop() {
      var originalMessage = qualityGateCalculationRequestEvent();

      byte[] serializedData =
        QualityGateCalculationEventSerdes.QualityGateCalculationRequestEvent()
          .serializer()
          .serialize("test-topic", originalMessage);
      assertThat(serializedData).isNotNull();

      io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent deserializedMessage =
        QualityGateCalculationEventSerdes.QualityGateCalculationRequestEvent()
          .deserializer()
          .deserialize("test-topic", serializedData);
      assertThat(deserializedMessage).isNotNull().isEqualTo(originalMessage);
    }
  }

  @Nested
  class OpenApiCoverageResponseEvent {

    @Test
    void serializationAndDeserializationLoop() {
      var originalMessage =
        io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent.builder();

      byte[] serializedData =
        QualityGateCalculationEventSerdes.OpenApiCoverageResponseEvent()
          .serializer()
          .serialize("test-topic", originalMessage);
      assertThat(serializedData).isNotNull();

      io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent deserializedMessage =
        QualityGateCalculationEventSerdes.OpenApiCoverageResponseEvent()
          .deserializer()
          .deserialize("test-topic", serializedData);
      assertThat(deserializedMessage).isNotNull().isEqualTo(originalMessage);
    }
  }
}

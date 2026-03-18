/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.defaultApiInformation;
import static io.github.bbortt.snow.white.microservices.openapi.coverage.stream.TestData.qualityGateCalculationRequestEvent;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QualityGateCalculationEventSerdesTest {

  @Nested
  class QualityGateCalculationRequestEventTest {

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
  class OpenApiCoverageResponseEventTest {

    @Test
    void serializationAndDeserializationLoop() {
      var originalMessage =
        new io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent(
          defaultApiInformation(),
          emptySet()
        );

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

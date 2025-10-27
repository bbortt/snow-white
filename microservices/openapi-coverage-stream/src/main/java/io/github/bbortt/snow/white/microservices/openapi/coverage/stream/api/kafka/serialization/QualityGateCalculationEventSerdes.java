/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.api.kafka.serialization;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.kafka.common.serialization.Serdes.serdeFrom;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

@NoArgsConstructor(access = PRIVATE)
public final class QualityGateCalculationEventSerdes {

  public static Serde<
    QualityGateCalculationRequestEvent
  > QualityGateCalculationRequestEvent() {
    var serializer = new JacksonJsonSerializer<
      QualityGateCalculationRequestEvent
    >();
    var deserializer = new JacksonJsonDeserializer<>(
      QualityGateCalculationRequestEvent.class
    );

    return serdeFrom(serializer, deserializer);
  }

  public static Serde<
    OpenApiCoverageResponseEvent
  > OpenApiCoverageResponseEvent() {
    var serializer = new JacksonJsonSerializer<OpenApiCoverageResponseEvent>();
    var deserializer = new JacksonJsonDeserializer<>(
      OpenApiCoverageResponseEvent.class
    );

    return serdeFrom(serializer, deserializer);
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.config;

import static org.apache.kafka.common.serialization.Serdes.serdeFrom;

import io.github.bbortt.snow.white.commons.event.OpenApiCoverageResponseEvent;
import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import org.apache.kafka.common.serialization.Serde;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@EnableKafka
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

  public static Serde<
    QualityGateCalculationRequestEvent
  > QualityGateCalculationRequestEvent() {
    JsonSerializer<QualityGateCalculationRequestEvent> serializer =
      new JsonSerializer<>();
    JsonDeserializer<QualityGateCalculationRequestEvent> deserializer =
      new JsonDeserializer<>(QualityGateCalculationRequestEvent.class);

    return serdeFrom(serializer, deserializer);
  }

  public static Serde<
    OpenApiCoverageResponseEvent
  > OpenApiCoverageResponseEvent() {
    JsonSerializer<OpenApiCoverageResponseEvent> serializer =
      new JsonSerializer<>();
    JsonDeserializer<OpenApiCoverageResponseEvent> deserializer =
      new JsonDeserializer<>(OpenApiCoverageResponseEvent.class);

    return serdeFrom(serializer, deserializer);
  }
}

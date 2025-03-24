package io.github.bbortt.snow.white.microservices.openapi.coverage.service.config;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverage;
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
    return org.apache.kafka.common.serialization.Serdes.serdeFrom(
      serializer,
      deserializer
    );
  }

  public static Serde<OpenApiCoverage> OpenApiCoverage() {
    JsonSerializer<OpenApiCoverage> serializer = new JsonSerializer<>();
    JsonDeserializer<OpenApiCoverage> deserializer = new JsonDeserializer<>(
      OpenApiCoverage.class
    );
    return org.apache.kafka.common.serialization.Serdes.serdeFrom(
      serializer,
      deserializer
    );
  }
}

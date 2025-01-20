package io.github.bbortt.snow.white.kafka.microservices.event.filter.config;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class KafkaStreamsConfigTest {

  private KafkaStreamsConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new KafkaStreamsConfig();
  }

  @Nested
  class SnowWhiteKafkaProperties {

    @Test
    void configuresSchemaRegistryUrl() {
      var kafkaEventFilterProperties = new KafkaEventFilterProperties();
      var schemaRegistryUrl = "mock://" + getClass().getSimpleName();
      kafkaEventFilterProperties.setSchemaRegistryUrl(schemaRegistryUrl);

      var properties = fixture.snowWhiteKafkaProperties(
        kafkaEventFilterProperties
      );

      assertThat(properties)
        .hasSize(2)
        .containsEntry(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
        .containsEntry(
          SPECIFIC_PROTOBUF_VALUE_TYPE,
          ExportTraceServiceRequest.class.getName()
        );
    }

    public static Stream<String> throwsExceptionWithoutSchemaRegistryUrl() {
      return Stream.of("", null);
    }

    @MethodSource
    @ParameterizedTest
    void throwsExceptionWithoutSchemaRegistryUrl(String schemaRegistryUrl) {
      var kafkaEventFilterProperties = new KafkaEventFilterProperties();
      kafkaEventFilterProperties.setSchemaRegistryUrl(schemaRegistryUrl);

      assertThatThrownBy(() ->
        fixture.snowWhiteKafkaProperties(kafkaEventFilterProperties)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Kafka schema registry URL is required!");
    }
  }
}

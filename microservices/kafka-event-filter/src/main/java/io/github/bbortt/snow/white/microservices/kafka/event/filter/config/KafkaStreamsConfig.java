/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.CONSUMER_MODE_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.PropertyUtils.propertiesToMap;
import static org.apache.kafka.common.serialization.Serdes.serdeFrom;
import static org.springframework.util.StringUtils.hasText;

import com.google.protobuf.util.JsonFormat;
import io.confluent.kafka.streams.serdes.protobuf.KafkaProtobufSerde;
import io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka.stream.exception.SerializationException;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.Properties;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@EnableKafka
@Configuration
@EnableKafkaStreams
public class KafkaStreamsConfig {

  public static Serde<ExportTraceServiceRequest> JsonSerde() {
    var parser = JsonFormat.parser().ignoringUnknownFields();
    var printer = JsonFormat.printer();

    var logger = LoggerFactory.getLogger("protobuf-jackson-bridge");

    Serializer<ExportTraceServiceRequest> serializer = (
      String topic,
      ExportTraceServiceRequest data
    ) -> {
      try {
        return printer.print(data).getBytes();
      } catch (Exception e) {
        logger.error("Error serializing protobuf JSON message", e);
        throw new SerializationException(
          "Error serializing protobuf JSON message",
          e
        );
      }
    };

    Deserializer<ExportTraceServiceRequest> deserializer = (
      String topic,
      byte[] data
    ) -> {
      try {
        var builder = ExportTraceServiceRequest.newBuilder();
        parser.merge(new String(data), builder);
        return builder.build();
      } catch (Exception e) {
        logger.error("Error deserializing protobuf JSON message", e);
        throw new SerializationException(
          "Error deserializing protobuf JSON message",
          e
        );
      }
    };

    return serdeFrom(serializer, deserializer);
  }

  @Bean
  @ConditionalOnProperty(
    name = CONSUMER_MODE_PROPERTY_NAME,
    havingValue = "protobuf"
  )
  public Properties snowWhiteKafkaProperties(
    KafkaEventFilterProperties kafkaEventFilterProperties
  ) {
    if (!hasText(kafkaEventFilterProperties.getSchemaRegistryUrl())) {
      throw new IllegalArgumentException(
        "Kafka schema registry URL is required!"
      );
    }

    Properties props = new Properties();
    props.put(
      SCHEMA_REGISTRY_URL_CONFIG,
      kafkaEventFilterProperties.getSchemaRegistryUrl()
    );
    props.put(
      SPECIFIC_PROTOBUF_VALUE_TYPE,
      ExportTraceServiceRequest.class.getName()
    );
    return props;
  }

  @Bean
  @ConditionalOnProperty(
    name = CONSUMER_MODE_PROPERTY_NAME,
    havingValue = "protobuf"
  )
  public Serde<ExportTraceServiceRequest> protobufSerde(
    @Qualifier("snowWhiteKafkaProperties") Properties snowWhiteKafkaProperties
  ) {
    var kafkaProtobufSerde = new KafkaProtobufSerde<
      ExportTraceServiceRequest
    >();
    kafkaProtobufSerde.configure(
      propertiesToMap(snowWhiteKafkaProperties),
      false
    );
    return kafkaProtobufSerde;
  }
}

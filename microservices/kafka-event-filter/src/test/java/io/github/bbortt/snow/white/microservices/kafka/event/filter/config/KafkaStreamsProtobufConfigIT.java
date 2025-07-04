/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.IntegrationTest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serde;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@Isolated
@DirtiesContext
@IntegrationTest
@TestPropertySource(
  properties = {
    "snow.white.kafka.event.filter.consumer-mode=protobuf",
    "snow.white.kafka.event.filter.schema-registry-url=mock://KafkaStreamsConfigIT",
  }
)
class KafkaStreamsProtobufConfigIT {

  @Autowired
  private KafkaEventFilterProperties kafkaEventFilterProperties;

  @Autowired
  private Properties snowWhiteKafkaProperties;

  @Autowired
  private Serde<ExportTraceServiceRequest> exportTraceServiceRequestSerde;

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private KafkaStreamsConfig fixture;

  @Test
  void jsonSerdeDoesNotExist() {
    assertThatThrownBy(() ->
      applicationContext.getBean("jsonSerde", Serde.class)
    ).isInstanceOf(NoSuchBeanDefinitionException.class);
  }

  @Test
  void snowWhiteKafkaPropertiesIsBean() {
    assertThat(snowWhiteKafkaProperties).isEqualTo(
      fixture.snowWhiteKafkaProperties(kafkaEventFilterProperties)
    );
  }

  @Test
  void protobufSerdeIsBean() {
    assertThat(exportTraceServiceRequestSerde).isEqualTo(
      fixture.protobufSerde(snowWhiteKafkaProperties)
    );
  }
}

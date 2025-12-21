/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.INBOUND_TOPIC_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.OUTBOUND_TOPIC_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.otel.event.filter.stream.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@TestPropertySource(
  properties = {
    "snow.white.otel.event.filter.api-index.base-url=http://localhost:8085",
  }
)
class OtelEventFilterStreamPropertiesIT {

  @Value("${" + INBOUND_TOPIC_PROPERTY_NAME + "}")
  private String inboundTopic;

  @Value("${" + OUTBOUND_TOPIC_PROPERTY_NAME + "}")
  private String outboundTopic;

  @Autowired
  private OtelEventFilterStreamProperties otelEventFilterStreamProperties;

  @Test
  void inboundTopicNameAndPropertyNameMatch() {
    assertThat(inboundTopic).isEqualTo(
      otelEventFilterStreamProperties.getInboundTopicName()
    );
  }

  @Test
  void outboundTopicNameAndPropertyNameMatch() {
    assertThat(outboundTopic).isEqualTo(
      otelEventFilterStreamProperties.getOutboundTopicName()
    );
  }
}

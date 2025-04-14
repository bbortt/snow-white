/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KafkaTopicManagerTest {

  public static final String TEST_RESPONSE_TOPIC =
    "KafkaTopicManagerTest:response";
  public static final String TEST_REQUEST_TOPIC =
    "KafkaTopicManagerTest:request";

  private KafkaTopicManager fixture;

  @BeforeEach
  void beforeEachSetup() {
    var openApiCoverageServiceProperties =
      new ReportCoordinationServiceProperties();
    openApiCoverageServiceProperties.setCalculationRequestTopic(
      TEST_REQUEST_TOPIC
    );
    openApiCoverageServiceProperties
      .getOpenapiCalculationResponse()
      .setTopic(TEST_RESPONSE_TOPIC);

    fixture = new KafkaTopicManager(openApiCoverageServiceProperties);
  }

  @Test
  void testInboundTopicCreation() {
    var inboundTopic = fixture.calculationRequestTopic();

    assertThat(inboundTopic.name()).isEqualTo(TEST_REQUEST_TOPIC);
  }

  @Test
  void testOutboundTopicCreation() {
    var outboundTopic = fixture.openapiCalculationResponseTopic();

    assertThat(outboundTopic.name()).isEqualTo(TEST_RESPONSE_TOPIC);
  }
}

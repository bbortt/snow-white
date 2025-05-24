/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.api.kafka;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

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
  void shouldBeEnabled_whenPropertyIsSet() {
    var contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(KafkaTopicManager.class);

    var reportCoordinationServicePropertiesMock = mock(
      ReportCoordinationServiceProperties.class
    );

    var openapiCalculationResponse = mock(
      ReportCoordinationServiceProperties.OpenapiCalculationResponse.class
    );

    doReturn(openapiCalculationResponse)
      .when(reportCoordinationServicePropertiesMock)
      .getOpenapiCalculationResponse();

    contextRunner
      .withBean(ReportCoordinationServiceProperties.class, () ->
        reportCoordinationServicePropertiesMock
      )
      .withPropertyValues(PREFIX + ".init-topics=true")
      .run(context ->
        assertThat(context)
          .asInstanceOf(type(AssertableApplicationContext.class))
          .satisfies(
            c -> assertThat(c).hasSingleBean(KafkaTopicManager.class),
            c ->
              assertThat(c)
                .getBean("calculationRequestTopic", NewTopic.class)
                .isNotNull(),
            c ->
              assertThat(c)
                .getBean("openapiCalculationResponseTopic", NewTopic.class)
                .isNotNull()
          )
      );

    verify(
      reportCoordinationServicePropertiesMock
    ).getCalculationRequestTopic();
    verify(openapiCalculationResponse).getTopic();
  }

  @Test
  void shouldNotBeEnabled_ifPropertyIsNotSet() {
    var contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(KafkaTopicManager.class);

    contextRunner.run(context ->
      assertThat(context).doesNotHaveBean(KafkaTopicManager.class)
    );
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

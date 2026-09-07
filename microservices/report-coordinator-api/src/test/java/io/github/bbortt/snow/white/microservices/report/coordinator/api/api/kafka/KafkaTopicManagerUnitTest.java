/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith({ MockitoExtension.class })
class KafkaTopicManagerUnitTest {

  @Mock
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  @Mock
  private ReportCoordinationServiceProperties.OpenapiCalculationResponse openapiCalculationResponseMock;

  @InjectMocks
  private KafkaTopicManager fixture;

  @Test
  void shouldBeEnabled_whenPropertyIsSet() {
    doReturn(openapiCalculationResponseMock)
      .when(reportCoordinationServicePropertiesMock)
      .getOpenapiCalculationResponse();

    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      KafkaTopicManager.class
    );

    doReturn(true).when(reportCoordinationServicePropertiesMock).isInitTopics();

    contextRunner
      .withBean(
        ReportCoordinationServiceProperties.class,
        () -> reportCoordinationServicePropertiesMock
      )
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
    verify(openapiCalculationResponseMock).getTopic();
  }

  @Test
  void shouldNotBeEnabled_ifPropertyIsNotSet() {
    var contextRunner = new ApplicationContextRunner()
      .withUserConfiguration(KafkaTopicManager.class)
      .withBean(
        ReportCoordinationServiceProperties.class,
        ReportCoordinationServiceProperties::new
      );

    contextRunner.run(context ->
      assertThat(context)
        .asInstanceOf(type(AssertableApplicationContext.class))
        .satisfies(
          c -> assertThat(c).hasSingleBean(KafkaTopicManager.class),
          c ->
            assertThat(c)
              .getBean("calculationRequestTopic")
              .isNotInstanceOf(NewTopic.class),
          c ->
            assertThat(c)
              .getBean("openapiCalculationResponseTopic")
              .isNotInstanceOf(NewTopic.class)
        )
    );
  }

  @Nested
  class CalculationRequestTopicTest {

    @Test
    void shouldReturnBean() {
      doReturn(true)
        .when(reportCoordinationServicePropertiesMock)
        .isInitTopics();

      var testRequestTopic = "KafkaTopicManagerTest:request";
      doReturn(testRequestTopic)
        .when(reportCoordinationServicePropertiesMock)
        .getCalculationRequestTopic();

      var inboundTopic = fixture.calculationRequestTopic();

      assertThat(inboundTopic.name()).isEqualTo(testRequestTopic);
    }

    @Test
    void shouldReturnNullBean_whenNotEnabled() {
      doReturn(false)
        .when(reportCoordinationServicePropertiesMock)
        .isInitTopics();

      assertThat(fixture.calculationRequestTopic()).isNull();
    }
  }

  @Nested
  class OpenapiCalculationResponseTopicTest {

    @Test
    void shouldReturnBean() {
      doReturn(true)
        .when(reportCoordinationServicePropertiesMock)
        .isInitTopics();

      doReturn(openapiCalculationResponseMock)
        .when(reportCoordinationServicePropertiesMock)
        .getOpenapiCalculationResponse();

      var testResponseTopic = "KafkaTopicManagerTest:response";
      doReturn(testResponseTopic)
        .when(openapiCalculationResponseMock)
        .getTopic();

      var outboundTopic = fixture.openapiCalculationResponseTopic();

      assertThat(outboundTopic.name()).isEqualTo(testResponseTopic);
    }

    @Test
    void shouldReturnNullBean_whenNotEnabled() {
      doReturn(false)
        .when(reportCoordinationServicePropertiesMock)
        .isInitTopics();

      assertThat(fixture.openapiCalculationResponseTopic()).isNull();
    }
  }
}

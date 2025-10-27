/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.api.kafka;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties.PREFIX;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.config.ReportCoordinationServiceProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;

@ExtendWith({ MockitoExtension.class })
class KafkaTopicManagerTest {

  @Mock
  private Environment environmentMock;

  @Mock
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  @Mock
  private ReportCoordinationServiceProperties.OpenapiCalculationResponse openapiCalculationResponseMock;

  private KafkaTopicManager fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new KafkaTopicManager(
      environmentMock,
      reportCoordinationServicePropertiesMock
    );
  }

  @Test
  void shouldBeEnabled_whenPropertyIsSet() {
    doReturn(openapiCalculationResponseMock)
      .when(reportCoordinationServicePropertiesMock)
      .getOpenapiCalculationResponse();

    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      KafkaTopicManager.class
    );

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
    verify(
      reportCoordinationServicePropertiesMock
    ).getCalculationRequestTopic();
    verify(openapiCalculationResponseMock).getTopic();
  }

  @Nested
  class CalculationRequestTopic {

    @Test
    void shouldReturnBean() {
      doReturn(TRUE)
        .when(environmentMock)
        .getProperty(PREFIX + ".init-topics", Boolean.class, FALSE);

      var testRequestTopic = "KafkaTopicManagerTest:request";
      doReturn(testRequestTopic)
        .when(reportCoordinationServicePropertiesMock)
        .getCalculationRequestTopic();

      var inboundTopic = fixture.calculationRequestTopic();

      assertThat(inboundTopic.name()).isEqualTo(testRequestTopic);
    }

    @Test
    void shouldReturnNullBean_whenNotEnabled() {
      doReturn(FALSE)
        .when(environmentMock)
        .getProperty(PREFIX + ".init-topics", Boolean.class, FALSE);

      assertThat(fixture.calculationRequestTopic()).isNull();
    }
  }

  @Nested
  class OpenapiCalculationResponseTopic {

    @Test
    void shouldReturnBean() {
      doReturn(TRUE)
        .when(environmentMock)
        .getProperty(PREFIX + ".init-topics", Boolean.class, FALSE);

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
      doReturn(FALSE)
        .when(environmentMock)
        .getProperty(PREFIX + ".init-topics", Boolean.class, FALSE);

      assertThat(fixture.openapiCalculationResponseTopic()).isNull();
    }
  }
}

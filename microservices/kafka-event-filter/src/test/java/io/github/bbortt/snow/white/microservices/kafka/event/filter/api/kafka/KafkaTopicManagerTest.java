/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.api.kafka;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.PREFIX;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties;
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
  private KafkaEventFilterProperties kafkaEventFilterPropertiesMock;

  private KafkaTopicManager fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new KafkaTopicManager(
      environmentMock,
      kafkaEventFilterPropertiesMock
    );
  }

  @Test
  void shouldBeEnabled_whenPropertyIsSet() {
    var contextRunner = new ApplicationContextRunner().withUserConfiguration(
      KafkaTopicManager.class
    );

    contextRunner
      .withBean(KafkaEventFilterProperties.class, () ->
        kafkaEventFilterPropertiesMock
      )
      .withPropertyValues(PREFIX + ".init-topics=true")
      .run(context ->
        assertThat(context)
          .asInstanceOf(type(AssertableApplicationContext.class))
          .satisfies(
            c -> assertThat(c).hasSingleBean(KafkaTopicManager.class),
            c ->
              assertThat(c).getBean("inboundTopic", NewTopic.class).isNotNull(),
            c ->
              assertThat(c).getBean("outboundTopic", NewTopic.class).isNotNull()
          )
      );

    verify(kafkaEventFilterPropertiesMock).getInboundTopicName();
    verify(kafkaEventFilterPropertiesMock).getOutboundTopicName();
  }

  @Nested
  class InboundTopic {

    @Test
    void shouldReturnBean() {
      doReturn(TRUE)
        .when(environmentMock)
        .getProperty(PREFIX + ".init-topics", Boolean.class, FALSE);

      var testInboundTopic = "KafkaTopicManagerTest:inbound";
      doReturn(testInboundTopic)
        .when(kafkaEventFilterPropertiesMock)
        .getInboundTopicName();

      NewTopic inboundTopic = fixture.inboundTopic();

      assertThat(inboundTopic.name()).isEqualTo(testInboundTopic);
    }

    @Test
    void shouldReturnNullBean_whenNotEnabled() {
      doReturn(FALSE)
        .when(environmentMock)
        .getProperty(PREFIX + ".init-topics", Boolean.class, FALSE);

      assertThat(fixture.inboundTopic()).isNull();
    }
  }

  @Nested
  class OutboundTopic {

    @Test
    void shouldReturnBean() {
      doReturn(TRUE)
        .when(environmentMock)
        .getProperty(PREFIX + ".init-topics", Boolean.class, FALSE);

      var testOutboundTopic = "KafkaTopicManagerTest:outbound";
      doReturn(testOutboundTopic)
        .when(kafkaEventFilterPropertiesMock)
        .getOutboundTopicName();

      NewTopic outboundTopic = fixture.outboundTopic();

      assertThat(outboundTopic.name()).isEqualTo(testOutboundTopic);
    }

    @Test
    void shouldReturnNullBean_whenNotEnabled() {
      doReturn(FALSE)
        .when(environmentMock)
        .getProperty(PREFIX + ".init-topics", Boolean.class, FALSE);

      assertThat(fixture.outboundTopic()).isNull();
    }
  }
}

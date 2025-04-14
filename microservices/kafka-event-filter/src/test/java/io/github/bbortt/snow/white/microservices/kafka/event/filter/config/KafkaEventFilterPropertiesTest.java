/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.kafka.event.filter.config;

import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.INBOUND_TOPIC_PROPERTY_NAME;
import static io.github.bbortt.snow.white.microservices.kafka.event.filter.config.KafkaEventFilterProperties.OUTBOUND_TOPIC_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.InitializingBean;

class KafkaEventFilterPropertiesTest {

  private KafkaEventFilterProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new KafkaEventFilterProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class AfterPropertiesSet {

    public static Stream<String> emptyAndNullString() {
      return Stream.of("", null);
    }

    @Test
    void shouldPassWhenBothPropertiesAreSet() {
      var inbound = "inbound";
      var outbound = "outbound";

      fixture.setInboundTopicName(inbound);
      fixture.setOutboundTopicName(outbound);

      assertDoesNotThrow(() -> fixture.afterPropertiesSet());

      assertThat(fixture).satisfies(
        f -> assertThat(f.getInboundTopicName()).isEqualTo(inbound),
        f -> assertThat(f.getOutboundTopicName()).isEqualTo(outbound)
      );
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowWhenInboundTopicIsEmptyOrNull(String inbound) {
      fixture.setInboundTopicName(inbound);
      fixture.setOutboundTopicName("outbound");

      assertAfterPropertiesSetThrows();
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowWhenOutboundTopicIsEmptyOrNull(String outbound) {
      fixture.setInboundTopicName("inbound");
      fixture.setOutboundTopicName(outbound);

      assertAfterPropertiesSetThrows();
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowWhenBothPropertiesAreEmptyOrNull(String value) {
      fixture.setInboundTopicName(value);
      fixture.setOutboundTopicName(value);

      assertAfterPropertiesSetThrows();
    }

    private void assertAfterPropertiesSetThrows() {
      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
          INBOUND_TOPIC_PROPERTY_NAME,
          OUTBOUND_TOPIC_PROPERTY_NAME
        );
    }
  }

  @Nested
  class SemanticConvention {

    @Test
    void defaultFilterPropertiesAlignWithSemanticConvention() {
      assertThat(fixture.getFiltering()).satisfies(
        f -> assertThat(f.getApiNameProperty()).isEqualTo("openapi.name"),
        f -> assertThat(f.getApiVersionProperty()).isEqualTo("openapi.version")
      );
    }
  }
}

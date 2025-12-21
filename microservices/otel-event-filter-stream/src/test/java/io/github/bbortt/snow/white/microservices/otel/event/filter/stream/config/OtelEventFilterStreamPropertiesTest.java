/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config;

import static io.github.bbortt.snow.white.microservices.otel.event.filter.stream.config.OtelEventFilterStreamProperties.ConsumerMode.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.InitializingBean;

class OtelEventFilterStreamPropertiesTest {

  private OtelEventFilterStreamProperties fixture;

  public static Stream<String> emptyAndNullString() {
    return Stream.of("", null);
  }

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OtelEventFilterStreamProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class ConsumerModeTest {

    @Test
    void shouldDefaultToProtobuf() {
      assertThat(fixture.getConsumerMode()).isEqualTo(JSON);
    }
  }

  @Nested
  class AfterPropertiesSetTest {

    @Nested
    class ApiIndexPropertiesTest {

      static Stream<String> emptyAndNullString() {
        return OtelEventFilterStreamPropertiesTest.emptyAndNullString();
      }

      @BeforeEach
      void beforeEachSetup() {
        fixture.setInboundTopicName("inbound");
        fixture.setOutboundTopicName("outbound");
      }

      @Test
      void shouldPass_whenBaseUrlIsSet() {
        fixture.getApiIndex().setBaseUrl("api-index");

        assertThatCode(() ->
          fixture.afterPropertiesSet()
        ).doesNotThrowAnyException();
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowException_whenBaseUrlIsEmptyOrNull(String baseUrl) {
        fixture.getApiIndex().setBaseUrl(baseUrl);

        assertThatThrownBy(() -> fixture.afterPropertiesSet())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
            "All properties must be configured - missing: [snow.white.otel.event.filter.api-index.base-url]."
          );
      }
    }

    @Nested
    class KafkaTopicTest {

      static Stream<String> emptyAndNullString() {
        return OtelEventFilterStreamPropertiesTest.emptyAndNullString();
      }

      @BeforeEach
      void beforeEachSetup() {
        fixture.getApiIndex().setBaseUrl("baseUrl");
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

        assertThatThrownBy(() -> fixture.afterPropertiesSet())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
            "All properties must be configured - missing: [snow.white.otel.event.filter.inbound-topic-name]."
          );
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowWhenOutboundTopicIsEmptyOrNull(String outbound) {
        fixture.setInboundTopicName("inbound");
        fixture.setOutboundTopicName(outbound);

        assertThatThrownBy(() -> fixture.afterPropertiesSet())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
            "All properties must be configured - missing: [snow.white.otel.event.filter.outbound-topic-name]."
          );
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowWhenBothPropertiesAreEmptyOrNull(String value) {
        fixture.setInboundTopicName(value);
        fixture.setOutboundTopicName(value);

        assertThatThrownBy(() -> fixture.afterPropertiesSet())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
            "All properties must be configured - missing: [snow.white.otel.event.filter.outbound-topic-name, snow.white.otel.event.filter.inbound-topic-name]."
          );
      }
    }
  }

  @Nested
  class SemanticConventionTest {

    @Test
    void defaultFilterPropertiesAlignWithSemanticConvention() {
      assertThat(fixture.getFiltering()).satisfies(
        f -> assertThat(f.getApiNameProperty()).isEqualTo("api.name"),
        f -> assertThat(f.getApiVersionProperty()).isEqualTo("api.version")
      );
    }
  }
}

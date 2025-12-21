/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.InitializingBean;

class OpenApiCoverageStreamPropertiesTest {

  private OpenApiCoverageStreamProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCoverageStreamProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class AfterPropertiesSetTest {

    @Nested
    class ApiIndexPropertiesTest {

      static Stream<String> emptyAndNullString() {
        return Stream.of("", null);
      }

      @BeforeEach
      void beforeEachSetup() {
        fixture.setCalculationRequestTopic("requestTopic");
        fixture.setOpenapiCalculationResponseTopic("responseTopic");
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
            "All properties must be configured - missing: [snow.white.openapi.coverage.stream.api-index.base-url]."
          );
      }
    }

    @Nested
    class KafkaPropertiesTest {

      @BeforeEach
      void beforeEachSetup() {
        fixture.getApiIndex().setBaseUrl("baseUrl");
      }

      @Test
      void doesNotThrowAnythingIfPropertiesSet() {
        fixture.setCalculationRequestTopic("calculationRequestTopic");
        fixture.setOpenapiCalculationResponseTopic(
          "openapiCalculationResponseTopic"
        );

        assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
      }

      @Test
      void throwsExceptionWithMissingCalculationRequestTopic() {
        fixture.setOpenapiCalculationResponseTopic(
          "openapiCalculationResponseTopic"
        );

        assertThatThrownBy(() -> fixture.afterPropertiesSet())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
            "All properties must be configured - missing: [snow.white.openapi.coverage.stream.calculation-request-topic]."
          );
      }

      @Test
      void throwsExceptionWithMissingOpenapiCalculationResponseTopic() {
        fixture.setCalculationRequestTopic("calculationRequestTopic");

        assertThatThrownBy(() -> fixture.afterPropertiesSet())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
            "All properties must be configured - missing: [snow.white.openapi.coverage.stream.openapi-calculation-response-topic]."
          );
      }
    }
  }
}

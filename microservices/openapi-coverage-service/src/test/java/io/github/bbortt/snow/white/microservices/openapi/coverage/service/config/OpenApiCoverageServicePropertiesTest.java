package io.github.bbortt.snow.white.microservices.openapi.coverage.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

class OpenApiCoverageServicePropertiesTest {

  private OpenApiCoverageServiceProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiCoverageServiceProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class AfterPropertiesSet {

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
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.openapi.coverage.servicecalculation-request-topic]."
        );
    }

    @Test
    void throwsExceptionWithMissingOpenapiCalculationResponseTopic() {
      fixture.setCalculationRequestTopic("calculationRequestTopic");

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.openapi.coverage.serviceopenapi-calculation-response-topic]."
        );
    }
  }
}

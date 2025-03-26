package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

class ReportCoordinationServicePropertiesTest {

  private ReportCoordinationServiceProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ReportCoordinationServiceProperties();
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
      fixture
        .getOpenapiCalculationResponse()
        .setTopic("openapiCalculationResponseTopic");
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
    }

    @Test
    void throwsExceptionWithMissingCalculationRequestTopic() {
      fixture
        .getOpenapiCalculationResponse()
        .setTopic("openapiCalculationResponseTopic");
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.report.coordination.service.calculation-request-topic]."
        );
    }

    @Test
    void throwsExceptionWithMissingOpenapiCalculationResponseTopic() {
      fixture.setCalculationRequestTopic("calculationRequestTopic");
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.report.coordination.service.openapi-calculation-response.topic]."
        );
    }

    @Test
    void throwsExceptionWithMissingPublicApiGatewayUrl() {
      fixture.setCalculationRequestTopic("calculationRequestTopic");
      fixture
        .getOpenapiCalculationResponse()
        .setTopic("openapiCalculationResponseTopic");
      fixture.setQualityGateApiUrl("qualityGateApiUrl");

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.report.coordination.service.public-api-gateway-url]."
        );
    }

    @Test
    void throwsExceptionWithMissingQualityGateApiUrl() {
      fixture.setCalculationRequestTopic("calculationRequestTopic");
      fixture
        .getOpenapiCalculationResponse()
        .setTopic("openapiCalculationResponseTopic");
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.report.coordination.service.quality-gate-api-url]."
        );
    }
  }
}

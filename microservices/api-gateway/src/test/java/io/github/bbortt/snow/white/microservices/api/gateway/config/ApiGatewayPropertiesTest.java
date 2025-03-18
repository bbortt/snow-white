package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

class ApiGatewayPropertiesTest {

  private ApiGatewayProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiGatewayProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class AfterPropertiesSet {

    @Test
    void doesNotThrowAnythingIfPropertiesSet() {
      fixture.setQualityGateApiUrl("qualityGateApiUrl");
      fixture.setReportCoordinationServiceUrl("reportCoordinationServiceUrl");

      assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
    }
  }

  @Test
  void throwsExceptionWithMissingQualityGateApiUrl() {
    fixture.setReportCoordinationServiceUrl("reportCoordinationServiceUrl");

    assertThatThrownBy(() -> fixture.afterPropertiesSet())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.api.gateway.quality-gate-api-url]."
      );
  }

  @Test
  void throwsExceptionWithMissingQReportCoordinationServiceUrl() {
    fixture.setQualityGateApiUrl("qualityGateApiUrl");

    assertThatThrownBy(() -> fixture.afterPropertiesSet())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.api.gateway.report-coordination-service-url]."
      );
  }
}

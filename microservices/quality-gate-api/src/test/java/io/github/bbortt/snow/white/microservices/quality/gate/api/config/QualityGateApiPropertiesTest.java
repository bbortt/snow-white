package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

class QualityGateApiPropertiesTest {

  private QualityGateApiProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateApiProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class AfterPropertiesSet {

    @Test
    void doesNotThrowAnythingIfPropertiesSet() {
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");

      assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
    }

    @Test
    void throwsExceptionWithMissingPublicApiGatewayUrl() {
      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [io.github.bbortt.snow.white.microservices.quality.gate.api.public-api-gateway-url]."
        );
    }
  }
}

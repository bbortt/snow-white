/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config.validation;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QualityGateApiPropertiesValidatorTest {

  private QualityGateApiProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new QualityGateApiProperties();
  }

  @Nested
  class AfterPropertiesSet {

    @Test
    void doesNotThrowAnythingIfPropertiesSet() {
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");

      assertThatNoException().isThrownBy(() ->
        new QualityGateApiPropertiesValidator(fixture)
      );
    }

    @Test
    void throwsExceptionWithMissingPublicApiGatewayUrl() {
      assertThatThrownBy(() -> new QualityGateApiPropertiesValidator(fixture))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [snow.white.quality.gate.api.public-api-gateway-url]."
        );
    }
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config.validation;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.api.index.config.ApiIndexProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiIndexPropertiesValidatorTest {

  private ApiIndexProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiIndexProperties();
  }

  @Nested
  class AfterPropertiesSetTest {

    @Test
    void doesNotThrowAnythingIfPropertiesSet() {
      fixture.setPublicApiGatewayUrl("publicApiGatewayUrl");

      assertThatNoException().isThrownBy(() ->
        new ApiIndexPropertiesValidator(fixture)
      );
    }

    @Test
    void shouldThrowException_withMissingPublicApiGatewayUrl() {
      assertThatThrownBy(() -> new ApiIndexPropertiesValidator(fixture))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "All properties must be configured - missing: [snow.white.api.index.public-api-gateway-url]."
        );
    }
  }
}

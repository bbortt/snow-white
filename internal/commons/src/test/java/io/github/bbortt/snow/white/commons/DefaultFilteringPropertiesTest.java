/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons;

import static io.github.bbortt.snow.white.commons.DefaultFilteringProperties.DEFAULT_API_NAME_ATTRIBUTE_KEY;
import static io.github.bbortt.snow.white.commons.DefaultFilteringProperties.DEFAULT_API_VERSION_ATTRIBUTE_KEY;
import static io.github.bbortt.snow.white.commons.DefaultFilteringProperties.DEFAULT_SERVICE_NAME_ATTRIBUTE_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultFilteringPropertiesTest {

  private DefaultFilteringProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new DefaultFilteringProperties() {};
  }

  @Test
  void shouldHaveDefaultValues() {
    assertThat(fixture.getApiNameAttributeKey()).isEqualTo(
      DEFAULT_API_NAME_ATTRIBUTE_KEY
    );
    assertThat(fixture.getApiVersionAttributeKey()).isEqualTo(
      DEFAULT_API_VERSION_ATTRIBUTE_KEY
    );
    assertThat(fixture.getServiceNameAttributeKey()).isEqualTo(
      DEFAULT_SERVICE_NAME_ATTRIBUTE_KEY
    );
  }
}

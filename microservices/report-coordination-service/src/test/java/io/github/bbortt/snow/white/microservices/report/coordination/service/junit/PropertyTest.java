/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.junit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PropertyTest {

  @Test
  void staticInitializer_shouldCreateProperty() {
    String name = "testName";
    String value = "testValue";

    var property = Property.property(name, value);

    assertThat(property)
      .isNotNull()
      .satisfies(
        p -> assertThat(p.getName()).isEqualTo(name),
        p -> assertThat(p.getValue()).isEqualTo(value)
      );
  }
}

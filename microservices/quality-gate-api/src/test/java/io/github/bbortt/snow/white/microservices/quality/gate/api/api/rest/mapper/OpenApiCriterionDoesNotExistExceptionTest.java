/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OpenApiCriterionDoesNotExistExceptionTest {

  @Test
  void constructorShouldCreateMessageFromLabel() {
    var fixture = new OpenApiCriterionDoesNotExistException("label");

    assertThat(fixture).hasMessage("OpenApi Criterion 'label' does not exist!");
  }
}

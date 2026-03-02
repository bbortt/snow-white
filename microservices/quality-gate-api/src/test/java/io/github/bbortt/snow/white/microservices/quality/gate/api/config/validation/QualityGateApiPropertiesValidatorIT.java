/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.quality.gate.api.AbstractQualityGateApiIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class QualityGateApiPropertiesValidatorIT extends AbstractQualityGateApiIT {

  @Autowired
  private QualityGateApiPropertiesValidator qualityGateApiPropertiesValidator;

  @Test
  void shouldBeRegisteredWithinSpringContext() {
    assertThat(qualityGateApiPropertiesValidator).isNotNull();
  }
}

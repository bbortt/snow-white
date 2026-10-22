/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.validation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OpenApiCoverageStreamPropertiesValidatorIT
  extends AbstractOpenApiCoverageServiceIT
{

  @Autowired
  private OpenApiCoverageStreamPropertiesValidator openApiCoverageStreamPropertiesValidator;

  @Test
  void shouldBeRegisteredWithinSpringContext() {
    assertThat(openApiCoverageStreamPropertiesValidator).isNotNull();
  }
}

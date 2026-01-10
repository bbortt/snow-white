/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenAPIParametersTest {

  @Nested
  class ConstructorTest {

    @Test
    void withSwaggerParseResult() {
      assertThat(
        new OpenAPIParameters(mock(SwaggerParseResult.class))
      ).hasAllNullFieldsOrPropertiesExcept("swaggerParseResult");
    }

    @Test
    void withLocationAndSwaggerParseResult() {
      assertThat(
        new OpenAPIParameters("sourceUrl", mock(SwaggerParseResult.class))
      ).hasNoNullFieldsOrPropertiesExcept("apiInformation");
    }

    @Test
    void allArgsConstructor() {
      assertThat(
        new OpenAPIParameters(
          "sourceUrl",
          mock(SwaggerParseResult.class),
          mock(ApiInformation.class)
        )
      ).hasNoNullFieldsOrProperties();
    }
  }
}

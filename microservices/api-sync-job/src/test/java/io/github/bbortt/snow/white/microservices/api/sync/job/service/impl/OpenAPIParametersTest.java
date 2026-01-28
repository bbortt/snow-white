/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service.impl;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenAPIParametersTest {

  @Mock
  private SwaggerParseResult swaggerParseResultMock;

  @Nested
  class ConstructorTest {

    @Test
    void withSwaggerParseResult() {
      assertThat(
        new OpenAPIParameters(swaggerParseResultMock)
      ).hasAllNullFieldsOrPropertiesExcept("swaggerParseResult");
    }

    @Test
    void withLocationAndSwaggerParseResult() {
      assertThat(
        new OpenAPIParameters("sourceUrl", swaggerParseResultMock)
      ).hasNoNullFieldsOrPropertiesExcept("apiInformation");
    }

    @Test
    void allArgsConstructor() {
      assertThat(
        new OpenAPIParameters(
          "sourceUrl",
          swaggerParseResultMock,
          mock(ApiInformation.class)
        )
      ).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class WithApiInformationTest {

    @Test
    void shouldCompleteClassWithApiInformation() {
      var fixture = new OpenAPIParameters(
        "sourceUrl",
        swaggerParseResultMock,
        null
      );

      assertThat(fixture).hasNoNullFieldsOrPropertiesExcept("apiInformation");

      var apiInformation = mock(ApiInformation.class);
      fixture = fixture.withApiInformation(apiInformation);

      assertThat(fixture).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class OpenApiAsJsonTest {

    @Test
    void shouldReturnOpenAPIFromSwaggerParseResultAsJson() {
      doReturn(
        new OpenAPI(SpecVersion.V31).info(
          new Info().title(getClass().getSimpleName())
        )
      )
        .when(swaggerParseResultMock)
        .getOpenAPI();

      var fixture = new OpenAPIParameters(
        "sourceUrl",
        swaggerParseResultMock,
        null
      );

      assertThat(fixture.openApiAsJson())
        .startsWith("{")
        .endsWith("}")
        .contains(format("\"title\":\"%s\"", getClass().getSimpleName()));
    }
  }
}

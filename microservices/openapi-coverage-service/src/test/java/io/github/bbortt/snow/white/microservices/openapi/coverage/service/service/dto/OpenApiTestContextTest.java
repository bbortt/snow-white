/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

class OpenApiTestContextTest {

  @Test
  void requiredArgsConstructor() {
    var apiInformation = mock(ApiInformation.class);
    var openAPI = mock(OpenAPI.class);
    var lookbackWindow = "lookbackWindow";

    assertThat(new OpenApiTestContext(apiInformation, openAPI, lookbackWindow))
      .isNotNull()
      .hasNoNullFieldsOrPropertiesExcept(
        "openTelemetryData",
        "openApiTestResults"
      );
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenApiConfigurationTest {

  private ApiIndexProperties apiIndexProperties;

  private OpenApiConfiguration fixture;

  @BeforeEach
  void beforeEachSetup() {
    apiIndexProperties = new ApiIndexProperties();

    fixture = new OpenApiConfiguration(apiIndexProperties);
  }

  @Nested
  class OpenApi {

    @Test
    void shouldConfigureOpenAPI() {
      String publicApiGatewayUrl = "https://api.example.com";
      apiIndexProperties.setPublicApiGatewayUrl(publicApiGatewayUrl);

      OpenAPI result = fixture.openApi();

      assertThat(result)
        .isNotNull()
        .satisfies(
          openAPI ->
            assertThat(openAPI.getInfo())
              .isNotNull()
              .extracting(Info::getTitle)
              .isEqualTo("API Index API"),
          openAPI ->
            assertThat(openAPI.getServers())
              .hasSize(1)
              .first()
              .isNotNull()
              .extracting(Server::getUrl)
              .isEqualTo(publicApiGatewayUrl)
        );
    }
  }
}

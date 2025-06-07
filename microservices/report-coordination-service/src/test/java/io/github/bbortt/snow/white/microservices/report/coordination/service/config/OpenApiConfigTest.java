/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class OpenApiConfigTest {

  @Mock
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  private OpenApiConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiConfig(reportCoordinationServicePropertiesMock);
  }

  @Test
  void constructorInitializesFields() {
    assertThat(fixture).hasNoNullFieldsOrProperties();
  }

  @Nested
  class OpenApi {

    @Test
    void shouldAppendPublicServerUrl() {
      var publicApiGatewayUrl = "publicApiGatewayUrl";
      doReturn(publicApiGatewayUrl)
        .when(reportCoordinationServicePropertiesMock)
        .getPublicApiGatewayUrl();

      assertThat(fixture.openApi())
        .isNotNull()
        .satisfies(
          o ->
            assertThat(o.getInfo())
              .isNotNull()
              .extracting(Info::getTitle)
              .isEqualTo("Report Coordination Service"),
          o ->
            assertThat(o.getServers())
              .isNotNull()
              .hasSize(1)
              .first()
              .extracting(Server::getUrl)
              .isEqualTo(publicApiGatewayUrl)
        );
    }
  }
}

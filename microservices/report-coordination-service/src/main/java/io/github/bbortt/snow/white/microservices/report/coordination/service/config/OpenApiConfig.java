/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static java.util.Collections.singletonList;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

  private final ReportCoordinationServiceProperties reportCoordinationServiceProperties;

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
      .info(new Info().title("Report Coordination Service"))
      .servers(
        singletonList(
          new Server()
            .url(reportCoordinationServiceProperties.getPublicApiGatewayUrl())
        )
      );
  }
}

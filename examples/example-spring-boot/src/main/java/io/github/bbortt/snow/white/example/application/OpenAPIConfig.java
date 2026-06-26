/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.example.application;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

  @Bean
  public OpenAPI pingPongOpenAPI() {
    return new OpenAPI().info(
      new Info()
        .title("Ping-Pong API")
        .version("1.0.0")
        .description(
          "A simple API for ping-pong interactions to demonstrate OpenAPI coverage calculation"
        )
        .extensions(
          Map.of(
            "x-api-name",
            "ping-pong",
            "x-service-name",
            "example-spring-boot"
          )
        )
    );
  }
}

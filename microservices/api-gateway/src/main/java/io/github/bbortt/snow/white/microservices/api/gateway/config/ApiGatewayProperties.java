/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static io.github.bbortt.snow.white.microservices.api.gateway.config.ApiGatewayProperties.PREFIX;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Getter
@Setter
@ConfigurationProperties(PREFIX)
@Configuration(proxyBeanMethods = false)
public class ApiGatewayProperties {

  public static final String PREFIX = "snow.white.api.gateway";

  private @Nullable Environment environment;

  private String contentSecurityPolicy =
    "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:";

  private String publicUrl;
  private String qualityGateApiUrl;
  private String reportCoordinatorApiUrl;
}

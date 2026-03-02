/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.config;

import static io.github.bbortt.snow.white.microservices.api.index.config.ApiIndexProperties.PREFIX;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = PREFIX)
@Configuration(proxyBeanMethods = false)
public class ApiIndexProperties {

  public static final String PREFIX = "snow.white.api.index";

  private String publicApiGatewayUrl;
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties.PREFIX;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = PREFIX)
@Configuration(proxyBeanMethods = false)
public class QualityGateApiProperties {

  public static final String PREFIX = "snow.white.quality.gate.api";

  private String publicApiGatewayUrl;
}

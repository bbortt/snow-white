/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.quality.gate.api.config.QualityGateApiProperties.PREFIX;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = PREFIX)
public class QualityGateApiProperties implements InitializingBean {

  public static final String PREFIX =
    "io.github.bbortt.snow.white.microservices.quality.gate.api";

  private String publicApiGatewayUrl;

  @Override
  public void afterPropertiesSet() {
    Map<String, String> fields = new HashMap<>();
    fields.put(PREFIX + ".public-api-gateway-url", publicApiGatewayUrl);

    assertRequiredProperties(fields);
  }
}

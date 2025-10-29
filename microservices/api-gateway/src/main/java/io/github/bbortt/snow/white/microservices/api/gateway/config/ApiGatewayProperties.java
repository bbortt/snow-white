/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.api.gateway.config.ApiGatewayProperties.PREFIX;
import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class ApiGatewayProperties implements InitializingBean, EnvironmentAware {

  public static final String PREFIX = "snow.white.api.gateway";

  private @Nullable Environment environment;

  private String contentSecurityPolicy =
    "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://storage.googleapis.com; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:";

  private String publicUrl;
  private String qualityGateApiUrl;
  private String reportCoordinatorApiUrl;

  @Override
  public void afterPropertiesSet() {
    Map<String, String> fields = new HashMap<>();
    fields.put(PREFIX + ".quality-gate-api-url", qualityGateApiUrl);
    fields.put(PREFIX + ".report-coordinator-api-url", reportCoordinatorApiUrl);

    if (nonNull(environment) && environment.acceptsProfiles(Profiles.of("prod"))) {
      fields.put(PREFIX + ".public-url", publicUrl);
    }

    assertRequiredProperties(fields);
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}

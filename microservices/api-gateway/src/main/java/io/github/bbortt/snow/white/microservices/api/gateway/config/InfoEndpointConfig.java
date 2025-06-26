/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class InfoEndpointConfig implements InfoContributor {

  private final Environment environment;

  public InfoEndpointConfig(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void contribute(Info.Builder builder) {
    builder.withDetail("activeProfiles", environment.getActiveProfiles());
  }
}

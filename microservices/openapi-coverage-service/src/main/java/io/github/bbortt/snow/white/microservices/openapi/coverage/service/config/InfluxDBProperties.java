/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.config;

import static org.springframework.util.StringUtils.hasText;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "influxdb")
public class InfluxDBProperties implements InitializingBean {

  private String url;
  private String token;
  private String org;
  private String bucket;

  @Override
  public void afterPropertiesSet() {
    if (!hasText(url) || !hasText(token) || !hasText(org) || !hasText(bucket)) {
      throw new IllegalArgumentException(
        "InfluxDB connection not properly configured! Please read the docs."
      );
    }
  }
}

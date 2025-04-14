/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.spring.web;

import static io.github.bbortt.snow.white.toolkit.spring.web.SnowWhiteAutoConfiguration.PROPERTY;
import static io.github.bbortt.snow.white.toolkit.spring.web.SnowWhiteAutoConfiguration.PROPERTY_PREFIX;

import io.github.bbortt.snow.white.toolkit.spring.web.config.InterceptorConfig;
import io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties;
import io.github.bbortt.snow.white.toolkit.spring.web.interceptor.OpenApiInformationEnhancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@Slf4j
@AutoConfiguration
@Import(
  {
    SpringWebInterceptorProperties.class,
    OpenApiInformationEnhancer.class,
    InterceptorConfig.class,
  }
)
@ConditionalOnProperty(
  prefix = PROPERTY_PREFIX,
  name = PROPERTY,
  havingValue = "true",
  matchIfMissing = true
)
public class SnowWhiteAutoConfiguration {

  static final String PROPERTY_PREFIX =
    "io.github.bbortt.snow.white.toolkit.spring.web";
  static final String PROPERTY = "enabled";

  public SnowWhiteAutoConfiguration() {
    logger.info("Enhancing OTEL Spans with Snow-White information ✅");
  }
}

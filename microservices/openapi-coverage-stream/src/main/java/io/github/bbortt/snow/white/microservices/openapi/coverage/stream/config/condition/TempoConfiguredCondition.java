/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.condition;

import static org.springframework.util.StringUtils.hasText;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Matches when Grafana Tempo is configured: a URL is set, together with either
 * a bearer token or a username/password pair for HTTP Basic authentication.
 */
public class TempoConfiguredCondition implements Condition {

  @Override
  public boolean matches(
    ConditionContext context,
    AnnotatedTypeMetadata metadata
  ) {
    var environment = context.getEnvironment();

    var urlPresent = hasText(environment.getProperty("tempo.url"));
    var tokenPresent = hasText(environment.getProperty("tempo.token"));
    var basicAuthPresent =
      hasText(environment.getProperty("tempo.username")) &&
      hasText(environment.getProperty("tempo.password"));

    return urlPresent && (tokenPresent || basicAuthPresent);
  }
}

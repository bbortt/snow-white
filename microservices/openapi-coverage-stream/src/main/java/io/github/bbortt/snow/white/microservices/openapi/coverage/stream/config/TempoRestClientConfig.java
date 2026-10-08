/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.condition.TempoConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
@Conditional(TempoConfiguredCondition.class)
public class TempoRestClientConfig {

  /**
   * Tempo's multi-tenancy header - see
   * <a href="https://grafana.com/docs/tempo/latest/operations/manage-advanced-systems/multitenancy/">Tempo multi-tenancy</a>
   */
  private static final String X_SCOPE_ORG_ID = "X-Scope-OrgID";

  @Bean
  public RestClient tempoRestClient(TempoProperties tempoProperties) {
    var builder = RestClient.builder().baseUrl(tempoProperties.getUrl());

    if (hasText(tempoProperties.getToken())) {
      builder.defaultHeader(
        AUTHORIZATION,
        "Bearer " + tempoProperties.getToken()
      );
    } else {
      builder.requestInterceptor(
        new BasicAuthenticationInterceptor(
          tempoProperties.getUsername(),
          tempoProperties.getPassword()
        )
      );
    }

    if (hasText(tempoProperties.getOrgId())) {
      builder.defaultHeader(X_SCOPE_ORG_ID, tempoProperties.getOrgId());
    }

    return builder.build();
  }
}

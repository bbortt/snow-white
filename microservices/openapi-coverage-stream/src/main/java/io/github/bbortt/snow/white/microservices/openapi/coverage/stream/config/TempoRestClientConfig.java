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

    return builder.build();
  }
}

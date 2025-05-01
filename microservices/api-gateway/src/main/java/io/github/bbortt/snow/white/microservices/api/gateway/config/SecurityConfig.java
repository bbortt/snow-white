/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.HTTPS_REDIRECT;
import static org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN;
import static org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.DENY;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import io.github.bbortt.snow.white.microservices.api.gateway.web.filter.SpaWebFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;

@Configuration
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
public class SecurityConfig {

  private final ApiGatewayProperties apiGatewayProperties;

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(
    ServerHttpSecurity http
  ) {
    http
      .securityMatcher(
        new NegatedServerWebExchangeMatcher(
          new OrServerWebExchangeMatcher(
            pathMatchers("/app/**", "/i18n/**", "/content/**")
          )
        )
      )
      .cors(withDefaults())
      .csrf(ServerHttpSecurity.CsrfSpec::disable)
      .addFilterAfter(new SpaWebFilter(), HTTPS_REDIRECT)
      .headers(headers ->
        headers
          .contentSecurityPolicy(csp ->
            csp.policyDirectives(
              apiGatewayProperties.getContentSecurityPolicy()
            )
          )
          .frameOptions(frameOptions -> frameOptions.mode(DENY))
          .referrerPolicy(referrer ->
            referrer.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
          )
          .permissionsPolicy(permissions ->
            permissions.policy(
              "camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"
            )
          )
      )
      .authorizeExchange(authz ->
        // prettier-ignore
                        authz
                                .pathMatchers("/").permitAll()
                                .pathMatchers("/*.*").permitAll()
                                .pathMatchers("/api/**").permitAll()
                                .pathMatchers("/swagger-ui/**").permitAll()
                                .pathMatchers("/v3/api-docs/**").permitAll()
                                .pathMatchers("/management/health").permitAll()
                                .pathMatchers("/management/health/**").permitAll()
                                .pathMatchers("/management/info").permitAll()
                                .pathMatchers("/management/**").denyAll()
      )
      .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable);
    return http.build();
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RoutingConfig {

  private final ApiGatewayProperties apiGatewayProperties;

  @Bean
  public RouteLocator snowWhiteRouteLocator(RouteLocatorBuilder builder) {
    return builder
      .routes()
      .route("report-coordination-service", r ->
        r
          .path("/api/rest/v1/quality-gates/*/calculate", "/api/rest/v1/reports/**")
          .uri(apiGatewayProperties.getReportCoordinationServiceUrl())
      )
      .route("quality-gate-api", r ->
        r.path("/api/rest/v1/criteria/**", "/api/rest/v1/quality-gates/**").uri(apiGatewayProperties.getQualityGateApiUrl())
      )
      .route("quality-gate-api-swagger", r ->
        r.path("/v3/api-docs/quality-gate-api").filters(apiDocsRewriteTarget()).uri(apiGatewayProperties.getQualityGateApiUrl())
      )
      .route("report-coordination-service-swagger", r ->
        r
          .path("/v3/api-docs/report-coordination-service")
          .filters(apiDocsRewriteTarget())
          .uri(apiGatewayProperties.getReportCoordinationServiceUrl())
      )
      .build();
  }

  private static Function<GatewayFilterSpec, UriSpec> apiDocsRewriteTarget() {
    return filterSpec -> filterSpec.rewritePath("/v3/api-docs/.+", "/v3/api-docs");
  }
}

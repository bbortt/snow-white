/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static java.lang.String.format;

import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
  public RouteLocator reportCoordinatorApi(RouteLocatorBuilder builder) {
    return builder
      .routes()
      .route("report-coordinator-api", r ->
        r.path("/api/rest/v1/quality-gates/*/calculate", "/api/rest/v1/reports/**").uri(apiGatewayProperties.getReportCoordinatorApiUrl())
      )
      .route("report-coordinator-api-swagger", r ->
        r.path("/v3/api-docs/report-coordinator-api").filters(apiDocsRewriteTarget()).uri(apiGatewayProperties.getReportCoordinatorApiUrl())
      )
      .build();
  }

  @Bean
  public RouteLocator qualityGateApi(RouteLocatorBuilder builder) {
    return builder
      .routes()
      .route("quality-gate-api", r ->
        r.path("/api/rest/v1/criteria/**", "/api/rest/v1/quality-gates/**").uri(apiGatewayProperties.getQualityGateApiUrl())
      )
      .route("quality-gate-api-swagger", r ->
        r.path("/v3/api-docs/quality-gate-api").filters(apiDocsRewriteTarget()).uri(apiGatewayProperties.getQualityGateApiUrl())
      )
      .build();
  }

  @Bean
  @ConditionalOnExpression("${server.port:8080} != ${management.server.port:${server.port:8080}}")
  public RouteLocator managementServer(
    RouteLocatorBuilder builder,
    @Value("${management.server.port:${server.port:8080}}") Integer managementServerPort
  ) {
    return builder
      .routes()
      .route("info-endpoint", r -> r.path("/management/info").uri(format("http://localhost:%d", managementServerPort)))
      .build();
  }

  private static Function<GatewayFilterSpec, UriSpec> apiDocsRewriteTarget() {
    return filterSpec -> filterSpec.rewritePath("/v3/api-docs/.+", "/v3/api-docs");
  }
}

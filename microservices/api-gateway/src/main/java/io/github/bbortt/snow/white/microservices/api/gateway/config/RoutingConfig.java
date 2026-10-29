/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static java.lang.String.format;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.NfTraceables;
import clew.traceables.clew.annotation.RealizesArch;
import clew.traceables.clew.annotation.RealizesNf;
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
@RealizesArch(ArchTraceables.ARCH_008_BACKEND_SERVICES_ADDRESSED_BY_PATH_PREFIX_WITH_AGGREGATED_OPENAPI_DOCS)
public class RoutingConfig {

  private final ApiGatewayProperties apiGatewayProperties;

  @Bean
  public RouteLocator apiIndexApi(RouteLocatorBuilder builder) {
    return builder
      .routes()
      .route("api-index-api", r -> r.path("/api/rest/v1/apis/**").uri(apiGatewayProperties.getApiIndexApiUrl()))
      .route("api-index-api-swagger", r ->
        r.path("/v3/api-docs/api-index-api").filters(apiDocsRewriteTarget()).uri(apiGatewayProperties.getApiIndexApiUrl())
      )
      .build();
  }

  /**
   * MCP is not REST and publishes no OpenAPI document, so this route has no {@code /v3/api-docs}
   * sibling. The path matches {@code spring.ai.mcp.server.streamable-http.mcp-endpoint}, which
   * {@code mcp-api} pins rather than inheriting from the framework.
   *
   * <p>This is the one backend route that may be absent: the MCP server is an opt-in deployment, so
   * an installation without one leaves {@code mcp-api-url} unset and {@code /mcp} is answered as a
   * path this gateway does not serve - rather than proxied to a backend that was never deployed,
   * which would answer the {@code 503} a deployed-but-unhealthy one does.
   */
  @Bean
  @RealizesNf(NfTraceables.NF_011_MCP_SERVER_IS_AN_OPT_IN_DEPLOYMENT)
  // Not @ConditionalOnProperty: that reads a value of `false` as absent, which would take the
  // route away behind a URL the validator accepted.
  @ConditionalOnExpression("'${" + ApiGatewayProperties.PREFIX + ".mcp-api-url:}' != ''")
  public RouteLocator mcpApi(RouteLocatorBuilder builder) {
    return builder
      .routes()
      .route("mcp-api", r -> r.path("/mcp", "/mcp/**").uri(apiGatewayProperties.getMcpApiUrl()))
      .build();
  }

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

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
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
      .route("quality-gate-api", r ->
        r
          .path("/api/rest/v1/quality-gates", "/api/rest/v1/quality-gates/**")
          .uri(apiGatewayProperties.getQualityGateApiUrl())
      )
      .route("report-coordination-service", r ->
        r
          .path("/api/rest/v1/reports/**")
          .uri(apiGatewayProperties.getReportCoordinationServiceUrl())
      )
      .build();
  }
}

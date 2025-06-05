/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.gateway.Main;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.gateway.quality-gate-api-url=http://localhost:8081",
    "snow.white.api.gateway.report-coordination-service-url=http://localhost:8084",
  }
)
class RoutingConfigIT {

  @Autowired
  private RouteLocator snowWhiteRouteLocator;

  @Autowired
  private ConfigurableApplicationContext applicationContextMock;

  @Autowired
  private RoutingConfig fixture;

  @Nested
  class SnowWhiteRouteLocator {

    @Test
    void isBean() {
      assertThat(snowWhiteRouteLocator).isNotNull();
    }

    @Test
    void configuresEndpoints() {
      RouteLocator routeLocator = fixture.snowWhiteRouteLocator(new RouteLocatorBuilder(applicationContextMock));

      var routes = routeLocator.getRoutes().collectList().block(Duration.ofSeconds(5));

      assertThat(routes)
        .hasSize(4)
        .satisfiesOnlyOnce(route ->
          assertThat(route).satisfies(
            r -> assertThat(r.getId()).isEqualTo("quality-gate-api"),
            r -> assertThat(r.getUri()).isEqualTo(new URI("http://localhost:8081"))
          )
        )
        .satisfiesOnlyOnce(route ->
          assertThat(route).satisfies(
            r -> assertThat(r.getId()).isEqualTo("quality-gate-api-swagger"),
            r -> assertThat(r.getUri()).isEqualTo(new URI("http://localhost:8081"))
          )
        )
        .satisfiesOnlyOnce(route ->
          assertThat(route).satisfies(
            r -> assertThat(r.getId()).isEqualTo("report-coordination-service"),
            r -> assertThat(r.getUri()).isEqualTo(new URI("http://localhost:8084"))
          )
        )
        .satisfiesOnlyOnce(route ->
          assertThat(route).satisfies(
            r -> assertThat(r.getId()).isEqualTo("report-coordination-service-swagger"),
            r -> assertThat(r.getUri()).isEqualTo(new URI("http://localhost:8084"))
          )
        );
    }
  }
}

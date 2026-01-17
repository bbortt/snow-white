/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.gateway.IntegrationTest;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;

@IntegrationTest
class RoutingConfigIT {

  @Autowired
  private RouteLocator snowWhiteRouteLocator;

  @Test
  void isBean() {
    assertThat(snowWhiteRouteLocator).isNotNull();
  }

  @Test
  void configuresEndpoints() {
    var routes = snowWhiteRouteLocator.getRoutes().collectList().block(Duration.ofSeconds(5));

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
          r -> assertThat(r.getId()).isEqualTo("report-coordinator-api"),
          r -> assertThat(r.getUri()).isEqualTo(new URI("http://localhost:8084"))
        )
      )
      .satisfiesOnlyOnce(route ->
        assertThat(route).satisfies(
          r -> assertThat(r.getId()).isEqualTo("report-coordinator-api-swagger"),
          r -> assertThat(r.getUri()).isEqualTo(new URI("http://localhost:8084"))
        )
      );
  }
}

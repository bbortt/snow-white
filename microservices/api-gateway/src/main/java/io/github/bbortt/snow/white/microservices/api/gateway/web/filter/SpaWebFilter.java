/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.web.filter;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

public class SpaWebFilter implements WebFilter {

  /**
   * Forwards any unmapped paths (except those containing a period) to the client {@code index.html}.
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    var path = exchange.getRequest().getURI().getPath();
    if (
      !path.startsWith("/api") &&
      !path.startsWith("/management") &&
      !path.startsWith("/swagger-ui") &&
      !path.startsWith("/v3/api-docs") &&
      !path.contains(".") &&
      path.matches("/(.*)")
    ) {
      return chain.filter(exchange.mutate().request(exchange.getRequest().mutate().path("/index.html").build()).build());
    }

    return chain.filter(exchange);
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class SpaWebFilterUnitTest {

  private AtomicReference<ServerWebExchange> forwardedExchange;
  private WebFilterChain chain;

  private SpaWebFilter fixture;

  @BeforeEach
  void setup() {
    forwardedExchange = new AtomicReference<>();
    chain = exchange -> {
      forwardedExchange.set(exchange);
      return Mono.empty();
    };

    fixture = new SpaWebFilter();
  }

  private String filterAndReturnForwardedPath(String path) {
    fixture.filter(MockServerWebExchange.from(MockServerHttpRequest.get(path)), chain).block();

    return forwardedExchange.get().getRequest().getURI().getPath();
  }

  @Nested
  class FilterTest {

    public static Stream<String> rewritesUnmappedPathToIndexHtml() {
      return Stream.of("/", "/api-index", "/first-level/second-level", "/1/2/3/4/5/6/7/8/9/10");
    }

    @MethodSource
    @ParameterizedTest
    @VerifiesSw(SwTraceables.SW_024_SPA_FALLBACK_REWRITES_UNMAPPED_ROUTES_TO_INDEX_HTML)
    void rewritesUnmappedPathToIndexHtml(String path) {
      assertThat(filterAndReturnForwardedPath(path)).isEqualTo("/index.html");
    }

    public static Stream<String> passesMappedPathThroughUntouched() {
      return Stream.of("/api/rest/v1/apis", "/management/health", "/swagger-ui/index.html", "/v3/api-docs/api-index-api", "/file.js");
    }

    @MethodSource
    @ParameterizedTest
    @VerifiesSw(SwTraceables.SW_024_SPA_FALLBACK_REWRITES_UNMAPPED_ROUTES_TO_INDEX_HTML)
    void passesMappedPathThroughUntouched(String path) {
      assertThat(filterAndReturnForwardedPath(path)).isEqualTo(path);
    }
  }
}

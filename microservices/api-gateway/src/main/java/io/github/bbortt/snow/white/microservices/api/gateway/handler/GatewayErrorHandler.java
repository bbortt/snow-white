/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.handler;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import io.github.bbortt.snow.white.commons.testing.VisibleForTesting;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

  @VisibleForTesting
  static final String ERROR_DETAILS_HEADER_NAME = "Gateway-Error";

  @VisibleForTesting
  static final String DOWNSTREAM_UNAVAILABLE_HEADER = "DOWNSTREAM_UNAVAILABLE";

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
    HttpStatus status = mapStatus(ex);

    exchange.getResponse().setStatusCode(status);

    if (isDownstreamUnavailable(ex)) {
      exchange.getResponse().getHeaders().add(ERROR_DETAILS_HEADER_NAME, DOWNSTREAM_UNAVAILABLE_HEADER);
    }

    return exchange.getResponse().setComplete();
  }

  private boolean isDownstreamUnavailable(Throwable ex) {
    return ex instanceof NoRouteToHostException || ex instanceof ConnectException || ex instanceof UnknownHostException;
  }

  private HttpStatus mapStatus(Throwable ex) {
    if (isDownstreamUnavailable(ex)) {
      return SERVICE_UNAVAILABLE;
    }

    if (ex instanceof TimeoutException) {
      return GATEWAY_TIMEOUT;
    }

    return INTERNAL_SERVER_ERROR;
  }
}

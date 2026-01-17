/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.handler;

import static io.github.bbortt.snow.white.microservices.api.gateway.handler.GatewayErrorHandler.DOWNSTREAM_UNAVAILABLE_HEADER;
import static io.github.bbortt.snow.white.microservices.api.gateway.handler.GatewayErrorHandler.ERROR_DETAILS_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith({ MockitoExtension.class })
class GatewayErrorHandlerTest {

  @Mock
  private ServerWebExchange serverWebExchangeMock;

  @Mock
  private ServerHttpResponse serverHttpResponseMock;

  private HttpHeaders httpHeaders;

  private GatewayErrorHandler fixture;

  @BeforeEach
  void setup() {
    httpHeaders = new HttpHeaders();

    doReturn(serverHttpResponseMock).when(serverWebExchangeMock).getResponse();
    doReturn(Mono.empty()).when(serverHttpResponseMock).setComplete();

    fixture = new GatewayErrorHandler();
  }

  @Nested
  class HandleTest {

    @Test
    void noRouteToHostException() {
      doReturn(httpHeaders).when(serverHttpResponseMock).getHeaders();

      fixture.handle(serverWebExchangeMock, new NoRouteToHostException("no route")).block();

      assertThat(httpHeaders.getFirst(ERROR_DETAILS_HEADER_NAME)).isEqualTo(DOWNSTREAM_UNAVAILABLE_HEADER);
      verify(serverHttpResponseMock).setStatusCode(SERVICE_UNAVAILABLE);
    }

    @Test
    void connectException() {
      doReturn(httpHeaders).when(serverHttpResponseMock).getHeaders();

      fixture.handle(serverWebExchangeMock, new ConnectException("refused")).block();

      assertThat(httpHeaders.getFirst(ERROR_DETAILS_HEADER_NAME)).isEqualTo(DOWNSTREAM_UNAVAILABLE_HEADER);
      verify(serverHttpResponseMock).setStatusCode(SERVICE_UNAVAILABLE);
    }

    @Test
    void unknownHostException() {
      doReturn(httpHeaders).when(serverHttpResponseMock).getHeaders();

      fixture.handle(serverWebExchangeMock, new UnknownHostException("unknown")).block();

      assertThat(httpHeaders.getFirst(ERROR_DETAILS_HEADER_NAME)).isEqualTo(DOWNSTREAM_UNAVAILABLE_HEADER);
      verify(serverHttpResponseMock).setStatusCode(SERVICE_UNAVAILABLE);
    }

    @Test
    void timeoutException() {
      fixture.handle(serverWebExchangeMock, new TimeoutException("timeout")).block();

      assertThat(httpHeaders.containsHeader(ERROR_DETAILS_HEADER_NAME)).isFalse();
      verify(serverHttpResponseMock).setStatusCode(GATEWAY_TIMEOUT);
    }

    @Test
    void unexpectedException() {
      fixture.handle(serverWebExchangeMock, new IllegalStateException("boom")).block();

      assertThat(httpHeaders.containsHeader(ERROR_DETAILS_HEADER_NAME)).isFalse();
      verify(serverHttpResponseMock).setStatusCode(INTERNAL_SERVER_ERROR);
    }
  }
}

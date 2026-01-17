package io.github.bbortt.snow.white.microservices.api.gateway.handler;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class GatewayErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
        HttpStatus status = mapStatus(throwable);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders()
                .add("Gateway-Error", "DOWNSTREAM_UNAVAILABLE");

        return exchange.getResponse().setComplete();
    }

    private HttpStatus mapStatus(Throwable throwable) {
        if (throwable instanceof NoRouteToHostException ||
                throwable instanceof ConnectException ||
                throwable instanceof UnknownHostException) {
            return SERVICE_UNAVAILABLE;
        }

        if (throwable instanceof TimeoutException) {
            return GATEWAY_TIMEOUT;
        }

        return INTERNAL_SERVER_ERROR;
    }
}

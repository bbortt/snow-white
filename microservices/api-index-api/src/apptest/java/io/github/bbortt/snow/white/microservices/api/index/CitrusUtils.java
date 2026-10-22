/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static lombok.AccessLevel.PRIVATE;

import io.github.bbortt.snow.white.toolkit.junit.jupiter.extension.TestIdentityBaggageInterceptor;
import lombok.NoArgsConstructor;
import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpEndpointConfiguration;

@NoArgsConstructor(access = PRIVATE)
final class CitrusUtils {

  static Endpoint getHttpEndpoint(String host, int port) {
    var endpointConfiguration = new HttpEndpointConfiguration();
    endpointConfiguration.setRequestUrl(format("http://%s:%s", host, port));
    // Names the running test on every request, so the service's spans carry it.
    endpointConfiguration.setClientInterceptors(
      singletonList(new TestIdentityBaggageInterceptor())
    );
    return new HttpClient(endpointConfiguration);
  }
}

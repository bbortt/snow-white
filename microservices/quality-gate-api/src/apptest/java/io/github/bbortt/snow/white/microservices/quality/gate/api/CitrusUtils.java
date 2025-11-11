/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.citrusframework.endpoint.Endpoint;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpEndpointConfiguration;

@NoArgsConstructor(access = PRIVATE)
final class CitrusUtils {

  static Endpoint getHttpEndpoint(String host, int port) {
    var endpointConfiguration = new HttpEndpointConfiguration();
    endpointConfiguration.setRequestUrl(format("http://%s:%s", host, port));
    return new HttpClient(endpointConfiguration);
  }
}

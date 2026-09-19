/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.config;

import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesCon;
import io.github.bbortt.snow.white.microservices.api.gateway.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

@IntegrationTest
@AutoConfigureWebTestClient
class SecurityConfigIT {

  @Autowired
  private WebTestClient webTestClient;

  /**
   * Only {@code /management/health(/**)} and {@code /management/info} are carved out of the
   * {@code /management/**} deny rule; every other actuator path must be rejected, whether or not
   * it is otherwise exposed by {@code management.endpoints.web.exposure.include}.
   */
  @Test
  @VerifiesCon(ConTraceables.CON_006_ACTUATOR_ENDPOINTS_DENY_BY_DEFAULT)
  void nonPermittedActuatorPathIsDenied() {
    webTestClient.get().uri("/management/env").exchange().expectStatus().isUnauthorized();
  }
}

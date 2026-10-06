/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway;

import static io.restassured.RestAssured.when;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.ConTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesCon;
import io.restassured.RestAssured;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ManagementEndpointsAppTest {

  @BeforeAll
  static void beforeAllSetup() {
    RestAssured.baseURI = format("http://%s", Optional.ofNullable(getProperty("api-gateway.host")).orElse("localhost"));
    RestAssured.port = parseInt(Optional.ofNullable(getProperty("api-gateway.port")).orElse("8080"));
  }

  /**
   * The API gateway shall route requests to the actuator info endpoint to the management server port.
   * <p>
   * When the client calls {@code /management/info}, the gateway must forward the request to {@code http://localhost:${management.port}/actuator/info} (on the management port).
   */
  @Test
  @VerifiesArch(ArchTraceables.ARCH_008_BACKEND_SERVICES_ADDRESSED_BY_PATH_PREFIX_WITH_AGGREGATED_OPENAPI_DOCS)
  @VerifiesCon(ConTraceables.CON_006_ACTUATOR_ENDPOINTS_DENY_BY_DEFAULT)
  void requestToInfoEndpointShouldSucceed() {
    when().get("/management/info").then().statusCode(200).body("activeProfiles", notNullValue()).body("activeProfiles.size()", is(2));
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static io.github.bbortt.snow.white.microservices.report.coordinator.api.CitrusUtils.getHttpEndpoint;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static org.springframework.http.HttpStatus.ACCEPTED;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.housekeeping.api.HousekeepingApi;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@CitrusSupport
class HousekeepingApiAppTest {

  private static HousekeepingApi housekeepingApi;

  @BeforeAll
  static void beforeAllSetup() {
    housekeepingApi = new HousekeepingApi(
      getHttpEndpoint(
        getProperty("report-coordinator-api.host", "localhost"),
        parseInt(getProperty("report-coordinator-api.port", "8084"))
      )
    );
  }

  /**
   * Verifies that triggering housekeeping returns HTTP 202 Accepted.
   *
   * <p>
   * When sending {@code POST /api/v1/housekeeping}, the service must:
   * <ul>
   *   <li>return HTTP 202 Accepted</li>
   *   <li>return an empty response body</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldTriggerHousekeeping(@CitrusResource TestCaseRunner testRunner) {
    testRunner.when(housekeepingApi.sendHousekeeping());
    testRunner.then(housekeepingApi.receiveHousekeeping(ACCEPTED));
  }
}

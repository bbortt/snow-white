/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.CitrusUtils.getHttpEndpoint;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static org.springframework.http.HttpStatus.OK;

import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.qualitygate.api.CriteriaApi;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@CitrusSupport
class CriteriaApiAppTest {
  // TODO: Citrus not supporting spring 7 at the moment
  //  private static CriteriaApi criteriaApi;
  //
  //  @BeforeAll
  //  static void beforeAllSetup() {
  //    criteriaApi = new CriteriaApi(
  //      getHttpEndpoint(
  //        getProperty("quality-gate-api.host", "localhost"),
  //        parseInt(getProperty("quality-gate-api.port", "8081"))
  //      )
  //    );
  //  }
  //
  //  @Test
  //  @CitrusTest
  //  void shouldGetAllOpenApiCriteria(@CitrusResource TestCaseRunner testRunner) {
  //    testRunner.when(criteriaApi.sendListOpenApiCriteria());
  //
  //    testRunner.then(criteriaApi.receiveListOpenApiCriteria(OK));
  //  }
}

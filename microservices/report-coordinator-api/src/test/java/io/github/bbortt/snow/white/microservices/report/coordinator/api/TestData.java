/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;

import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.microservices.report.coordinator.api.domain.model.ApiTest;

public final class TestData {

  public static ApiInformation defaultApiInformation() {
    return ApiInformation.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .apiType(OPENAPI)
      .build();
  }

  public static ApiTest defaultApiTest() {
    return ApiTest.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .apiType(OPENAPI.getVal())
      .build();
  }
}

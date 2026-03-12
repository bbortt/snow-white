/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;
import io.github.bbortt.snow.white.commons.quality.gate.ApiType;

public final class TestData {

  public static final String SERVICE_NAME = "serviceName";
  public static final String API_NAME = "apiName";
  public static final String API_VERSION = "apiVersion";

  public static final String LOOKBACK_WINDOW = "1h";

  public static ApiInformation defaultApiInformation() {
    return ApiInformation.builder()
      .serviceName("serviceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .apiType(OPENAPI)
      .build();
  }

  public static QualityGateCalculationRequestEvent qualityGateCalculationRequestEvent() {
    return qualityGateCalculationRequestEvent(OPENAPI);
  }

  public static QualityGateCalculationRequestEvent qualityGateCalculationRequestEvent(
    ApiType apiType
  ) {
    return QualityGateCalculationRequestEvent.builder()
      .apiInformation(
        ApiInformation.builder()
          .serviceName(SERVICE_NAME)
          .apiName(API_NAME)
          .apiVersion(API_VERSION)
          .apiType(apiType)
          .build()
      )
      .lookbackWindow(LOOKBACK_WINDOW)
      .build();
  }
}

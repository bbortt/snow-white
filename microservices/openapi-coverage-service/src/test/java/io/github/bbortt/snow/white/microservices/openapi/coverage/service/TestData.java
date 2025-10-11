/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service;

import io.github.bbortt.snow.white.commons.event.QualityGateCalculationRequestEvent;
import io.github.bbortt.snow.white.commons.event.dto.ApiInformation;

public class TestData {

  public static final String SERVICE_NAME = "serviceName";
  public static final String API_NAME = "apiName";
  public static final String API_VERSION = "apiVersion";

  public static final String LOOKBACK_WINDOW = "1h";

  public static QualityGateCalculationRequestEvent qualityGateCalculationRequestEvent() {
    return QualityGateCalculationRequestEvent.builder()
      .apiInformation(
        ApiInformation.builder()
          .serviceName(SERVICE_NAME)
          .apiName(API_NAME)
          .apiVersion(API_VERSION)
          .build()
      )
      .lookbackWindow(LOOKBACK_WINDOW)
      .build();
  }
}

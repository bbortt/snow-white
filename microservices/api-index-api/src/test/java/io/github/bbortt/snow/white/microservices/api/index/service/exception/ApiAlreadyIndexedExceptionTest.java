/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.index.service.exception;

import static io.github.bbortt.snow.white.microservices.api.index.api.rest.dto.GetAllApis200ResponseInner.ApiTypeEnum.UNSPECIFIED;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.api.index.domain.model.ApiReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiAlreadyIndexedExceptionTest {

  private ApiReference apiReference;

  @BeforeEach
  void beforeEachSetup() {
    apiReference = ApiReference.builder()
      .otelServiceName("otelServiceName")
      .apiName("apiName")
      .apiVersion("apiVersion")
      .sourceUrl("sourceUrl")
      .apiType(UNSPECIFIED)
      .build();
  }

  @Test
  void constructsMessage() {
    assertThat(new ApiAlreadyIndexedException(apiReference)).hasMessage(
      "API { otelServiceName=\"otelServiceName\", apiName=\"apiName\", apiVersion=\"apiVersion\"} already indexed!"
    );
  }
}

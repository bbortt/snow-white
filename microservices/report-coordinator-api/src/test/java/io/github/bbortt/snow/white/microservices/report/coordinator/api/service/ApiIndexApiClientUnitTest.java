/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.service;

import static org.mockito.Mockito.verify;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.api.client.apiindexapi.api.ApiIndexApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class ApiIndexApiClientUnitTest {

  @Mock
  private ApiIndexApi apiIndexApiMock;

  private ApiIndexApiClient fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiIndexApiClient(apiIndexApiMock);
  }

  @Nested
  class GetApiDetailsWithHttpInfoTest {

    @Test
    void shouldCallUnderlyingApiIndexApi() {
      var serviceName = "serviceName";
      var apiName = "apiName";
      var apiVersion = "apiVersion";

      fixture.getApiDetailsWithHttpInfo(serviceName, apiName, apiVersion);

      verify(apiIndexApiMock).getApiDetailsWithHttpInfo(
        serviceName,
        apiName,
        apiVersion
      );
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.ApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith({ MockitoExtension.class })
class QualityGateApiConfigTest {

  private static final String BASE_URL = "http://localhost:8080";

  @Mock
  private ReportCoordinationServiceProperties reportCoordinationServicePropertiesMock;

  private QualityGateApiConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    doReturn(BASE_URL)
      .when(reportCoordinationServicePropertiesMock)
      .getQualityGateApiUrl();

    fixture = new QualityGateApiConfig(reportCoordinationServicePropertiesMock);
  }

  @Nested
  class Constructor {

    @Test
    void shouldExtractQualityGateApiUrl() {
      assertThat(fixture).hasNoNullFieldsOrProperties();
    }
  }

  @Nested
  class QualityGateApi {

    @Mock
    private RestClient.Builder builderMock;

    @Test
    void shouldBeBuilt() {
      assertThat(fixture.qualityGateApi(builderMock))
        .isNotNull()
        .extracting(
          io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.api.QualityGateApi::getApiClient
        )
        .extracting(ApiClient::getBasePath)
        .isEqualTo(BASE_URL);
    }
  }
}

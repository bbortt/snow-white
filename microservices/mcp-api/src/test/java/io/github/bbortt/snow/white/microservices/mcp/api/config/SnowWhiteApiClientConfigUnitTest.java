/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.mcp.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.mcp.api.api.client.apiindexapi.api.ApiIndexApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.criteriaapi.api.CriteriaApi;
import io.github.bbortt.snow.white.microservices.mcp.api.api.client.reportapi.api.ReportApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

/**
 * Three documents, three {@code ApiClient} classes, three base URLs — and each client has to
 * end up with its own, because they are different services. Crossed wiring would answer every
 * tool from whichever service happened to be first.
 */
@ExtendWith({ MockitoExtension.class })
class SnowWhiteApiClientConfigUnitTest {

  private static final String API_INDEX_BASE_URL = "http://api-index";
  private static final String CRITERIA_BASE_URL = "http://quality-gate";
  private static final String REPORT_BASE_URL = "http://report-coordinator";

  @Mock
  private RestClient restClientMock;

  private final SnowWhiteApiClientConfig fixture = new SnowWhiteApiClientConfig(
    properties()
  );

  private static McpApiProperties properties() {
    var properties = new McpApiProperties();
    properties.getApiIndex().setBaseUrl(API_INDEX_BASE_URL);
    properties.getCriteria().setBaseUrl(CRITERIA_BASE_URL);
    properties.getReport().setBaseUrl(REPORT_BASE_URL);
    return properties;
  }

  @Test
  void apiIndexApiReadsTheApiIndexBaseUrl() {
    assertThat(fixture.apiIndexApi(restClientMock))
      .isInstanceOf(ApiIndexApi.class)
      .extracting(apiIndexApi -> apiIndexApi.getApiClient().getBasePath())
      .isEqualTo(API_INDEX_BASE_URL);
  }

  @Test
  void criteriaApiReadsTheCriteriaBaseUrl() {
    assertThat(fixture.criteriaApi(restClientMock))
      .isInstanceOf(CriteriaApi.class)
      .extracting(criteriaApi -> criteriaApi.getApiClient().getBasePath())
      .isEqualTo(CRITERIA_BASE_URL);
  }

  @Test
  void reportApiReadsTheReportBaseUrl() {
    assertThat(fixture.reportApi(restClientMock))
      .isInstanceOf(ReportApi.class)
      .extracting(reportApi -> reportApi.getApiClient().getBasePath())
      .isEqualTo(REPORT_BASE_URL);
  }
}

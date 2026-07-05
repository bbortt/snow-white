/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.influxdb.client.BucketsApi;
import com.influxdb.client.InfluxDBClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith({ MockitoExtension.class })
class InfluxDBClientConfigUnitTest {

  private InfluxDBClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new InfluxDBClientConfig();
  }

  @Nested
  class ValidateOnStartupTest {

    @Mock
    private ApplicationReadyEvent applicationReadyEventMock;

    @Mock
    private ConfigurableApplicationContext applicationContextMock;

    @Mock
    private InfluxDBClient influxDBClientMock;

    @Mock
    private BucketsApi bucketsApiMock;

    @BeforeEach
    void beforeEachSetup() {
      doReturn(applicationContextMock)
        .when(applicationReadyEventMock)
        .getApplicationContext();

      doReturn(true)
        .when(applicationContextMock)
        .containsBean("influxDBClient");

      doReturn(influxDBClientMock)
        .when(applicationContextMock)
        .getBean("influxDBClient", InfluxDBClient.class);
    }

    @Test
    void shouldPassSilently_whenInfluxDbIsConnected() {
      doReturn(true).when(influxDBClientMock).ping();
      doReturn(bucketsApiMock).when(influxDBClientMock).getBucketsApi();

      assertThatCode(() ->
        fixture.validateOnStartup(applicationReadyEventMock)
      ).doesNotThrowAnyException();

      verify(influxDBClientMock).ping();
      verify(bucketsApiMock).findBuckets();
    }

    @Test
    void shouldThrowException_whenPingFails() {
      doReturn(false).when(influxDBClientMock).ping();

      assertThatThrownBy(() ->
        fixture.validateOnStartup(applicationReadyEventMock)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Failed to connect to InfluxDB!")
        .rootCause()
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("InfluxDB ping failed — server not reachable");
    }

    @Test
    void shouldThrowException_whenCredentialsAreInvalid() {
      doReturn(true).when(influxDBClientMock).ping();
      doReturn(bucketsApiMock).when(influxDBClientMock).getBucketsApi();

      var rootCause = new IllegalArgumentException();
      doThrow(rootCause).when(bucketsApiMock).findBuckets();

      assertThatThrownBy(() ->
        fixture.validateOnStartup(applicationReadyEventMock)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Failed to connect to InfluxDB!")
        .rootCause()
        .isEqualTo(rootCause);
    }
  }
}

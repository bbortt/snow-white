/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.impl.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.exceptions.InternalServerErrorException;
import com.influxdb.exceptions.NotFoundException;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.annotation.EnableRetry;
import retrofit2.Response;

/**
 * The InfluxDB Java client wraps transport failures and 5xx responses alike as {@code InfluxException}
 * (no dedicated subclass for transport failures - {@code status() == 0}), while 4xx responses use a
 * dedicated subclass carrying a real {@code Response} (see {@code InfluxDBQueryClient}'s javadoc).
 * These tests exercise the real exception constructors with realistic arguments to keep that
 * distinction honest, rather than a bare Mockito stub of an arbitrary exception type.
 */
@SpringBootTest(classes = InfluxDBQueryClientRetryIT.TestConfig.class)
class InfluxDBQueryClientRetryIT {

  @EnableRetry
  static class TestConfig {

    @Bean
    QueryApi queryApi() {
      return mock(QueryApi.class);
    }

    @Bean
    InfluxDBClient influxDBClient(QueryApi queryApi) {
      var influxDBClient = mock(InfluxDBClient.class);
      doReturn(queryApi).when(influxDBClient).getQueryApi();
      return influxDBClient;
    }

    @Bean
    InfluxDBQueryClient influxDBQueryClient(InfluxDBClient influxDBClient) {
      return new InfluxDBQueryClient(influxDBClient);
    }
  }

  @Autowired
  private InfluxDBQueryClient fixture;

  @Autowired
  private QueryApi queryApiMock;

  @BeforeEach
  void beforeEachSetup() {
    reset(queryApiMock);
  }

  private static Response<Object> errorResponse(int code) {
    return Response.error(
      code,
      ResponseBody.create(MediaType.parse("application/json"), "{}")
    );
  }

  @Nested
  class QueryTest {

    @Test
    void shouldRetryBeforePropagatingFailure_on5xxResponse() {
      doThrow(new InternalServerErrorException(errorResponse(500)))
        .when(queryApiMock)
        .query(anyString());

      assertThatThrownBy(() -> fixture.query("flux")).isInstanceOf(
        InternalServerErrorException.class
      );

      verify(queryApiMock, times(3)).query(anyString());
    }

    @Test
    void shouldRetryBeforePropagatingFailure_onConnectivityFailure() {
      doThrow(new InfluxException(new IOException("connection refused")))
        .when(queryApiMock)
        .query(anyString());

      assertThatThrownBy(() -> fixture.query("flux")).isInstanceOf(
        InfluxException.class
      );

      verify(queryApiMock, times(3)).query(anyString());
    }

    @Test
    void shouldNotRetry_on4xxResponse() {
      doThrow(new NotFoundException(errorResponse(404)))
        .when(queryApiMock)
        .query(anyString());

      assertThatThrownBy(() -> fixture.query("flux")).isInstanceOf(
        NotFoundException.class
      );

      verify(queryApiMock, times(1)).query(anyString());
    }
  }
}

/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.InfluxDBProperties;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.TempoProperties;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class TelemetryBackendPropertiesValidatorUnitTest {

  private static final String NOT_CONFIGURED_MESSAGE =
    "No telemetry backend configured! Configure either InfluxDB or Grafana Tempo. Please read the docs.";

  private static final String AMBIGUOUS_MESSAGE =
    "Multiple telemetry backends configured! Configure either InfluxDB or Grafana Tempo, not both. Please read the docs.";

  private static final String INFLUXDB_INVALID_MESSAGE =
    "InfluxDB connection not properly configured! Please read the docs.";

  private static final String TEMPO_INVALID_MESSAGE =
    "Grafana Tempo connection not properly configured! Configure a URL and exactly one authentication method (token, or username/password). Please read the docs.";

  private InfluxDBProperties influxDBProperties;
  private TempoProperties tempoProperties;

  @BeforeEach
  void beforeEachSetup() {
    influxDBProperties = new InfluxDBProperties();
    tempoProperties = new TempoProperties();
  }

  private void assertThatValidatorThrows(String message) {
    assertThatThrownBy(() ->
      new TelemetryBackendPropertiesValidator(
        influxDBProperties,
        tempoProperties
      )
    )
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(message);
  }

  private void assertThatValidatorDoesNotThrow() {
    assertThatCode(() ->
      new TelemetryBackendPropertiesValidator(
        influxDBProperties,
        tempoProperties
      )
    ).doesNotThrowAnyException();
  }

  private void fullyConfigureInfluxDB() {
    influxDBProperties.setUrl("url");
    influxDBProperties.setToken("token");
    influxDBProperties.setOrg("org");
    influxDBProperties.setBucket("bucket");
  }

  private void fullyConfigureTempoWithToken() {
    tempoProperties.setUrl("url");
    tempoProperties.setToken("token");
  }

  private void fullyConfigureTempoWithBasicAuth() {
    tempoProperties.setUrl("url");
    tempoProperties.setUsername("username");
    tempoProperties.setPassword("password");
  }

  @Nested
  class NeitherBackendConfiguredTest {

    @Test
    void shouldThrow_whenNothingConfigured() {
      assertThatValidatorThrows(NOT_CONFIGURED_MESSAGE);
    }
  }

  @Nested
  class SingleBackendConfiguredTest {

    @Test
    void shouldNotThrow_whenOnlyInfluxDBFullyConfigured() {
      fullyConfigureInfluxDB();

      assertThatValidatorDoesNotThrow();
    }

    @Test
    void shouldNotThrow_whenOnlyTempoFullyConfiguredWithToken() {
      fullyConfigureTempoWithToken();

      assertThatValidatorDoesNotThrow();
    }

    @Test
    void shouldNotThrow_whenOnlyTempoFullyConfiguredWithBasicAuth() {
      fullyConfigureTempoWithBasicAuth();

      assertThatValidatorDoesNotThrow();
    }
  }

  @Nested
  class BothBackendsConfiguredTest {

    @Test
    void shouldThrow_whenInfluxDBAndTempoTokenBothConfigured() {
      fullyConfigureInfluxDB();
      fullyConfigureTempoWithToken();

      assertThatValidatorThrows(AMBIGUOUS_MESSAGE);
    }

    @Test
    void shouldThrow_whenInfluxDBAndTempoBasicAuthBothConfigured() {
      fullyConfigureInfluxDB();
      fullyConfigureTempoWithBasicAuth();

      assertThatValidatorThrows(AMBIGUOUS_MESSAGE);
    }
  }

  @Nested
  class InfluxDBPartiallyConfiguredTest {

    public static Stream<String> emptyOrNullString() {
      return Stream.of(null, "", " ");
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullString")
    void shouldThrow_whenUrlIsEmpty(String url) {
      fullyConfigureInfluxDB();
      influxDBProperties.setUrl(url);

      assertThatValidatorThrows(INFLUXDB_INVALID_MESSAGE);
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullString")
    void shouldThrow_whenTokenIsEmpty(String token) {
      fullyConfigureInfluxDB();
      influxDBProperties.setToken(token);

      assertThatValidatorThrows(INFLUXDB_INVALID_MESSAGE);
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullString")
    void shouldThrow_whenOrgIsEmpty(String org) {
      fullyConfigureInfluxDB();
      influxDBProperties.setOrg(org);

      assertThatValidatorThrows(INFLUXDB_INVALID_MESSAGE);
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullString")
    void shouldThrow_whenBucketIsEmpty(String bucket) {
      fullyConfigureInfluxDB();
      influxDBProperties.setBucket(bucket);

      assertThatValidatorThrows(INFLUXDB_INVALID_MESSAGE);
    }
  }

  @Nested
  class TempoPartiallyConfiguredTest {

    @Test
    void shouldThrow_whenUrlMissingButTokenSet() {
      tempoProperties.setToken("token");

      assertThatValidatorThrows(TEMPO_INVALID_MESSAGE);
    }

    @Test
    void shouldThrow_whenUrlMissingButBasicAuthSet() {
      tempoProperties.setUsername("username");
      tempoProperties.setPassword("password");

      assertThatValidatorThrows(TEMPO_INVALID_MESSAGE);
    }

    @Test
    void shouldThrow_whenUsernameSetWithoutPassword() {
      tempoProperties.setUrl("url");
      tempoProperties.setUsername("username");

      assertThatValidatorThrows(TEMPO_INVALID_MESSAGE);
    }

    @Test
    void shouldThrow_whenPasswordSetWithoutUsername() {
      tempoProperties.setUrl("url");
      tempoProperties.setPassword("password");

      assertThatValidatorThrows(TEMPO_INVALID_MESSAGE);
    }

    @Test
    void shouldThrow_whenBothTokenAndBasicAuthSet() {
      tempoProperties.setUrl("url");
      tempoProperties.setToken("token");
      tempoProperties.setUsername("username");
      tempoProperties.setPassword("password");

      assertThatValidatorThrows(TEMPO_INVALID_MESSAGE);
    }
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.InitializingBean;

class InfluxDBPropertiesTest {

  private InfluxDBProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new InfluxDBProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class AfterPropertiesSet {

    public static Stream<String> emptyOrNullString() {
      return Stream.of(null, "", " ");
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullString")
    void shouldThrow_whenUrlIsEmpty(String url) {
      fixture.setUrl(url);

      fixture.setToken("token");
      fixture.setOrg("org");
      fixture.setBucket("bucket");

      assertThatConnectionExceptionIsBeingThrown();
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullString")
    void shouldThrow_whenTokenIsEmpty(String token) {
      fixture.setUrl("url");

      fixture.setToken(token);

      fixture.setOrg("org");
      fixture.setBucket("bucket");

      assertThatConnectionExceptionIsBeingThrown();
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullString")
    void shouldThrow_whenOrgIsEmpty(String org) {
      fixture.setUrl("url");
      fixture.setToken("token");

      fixture.setOrg(org);

      fixture.setBucket("bucket");

      assertThatConnectionExceptionIsBeingThrown();
    }

    @ParameterizedTest
    @MethodSource("emptyOrNullString")
    void shouldThrow_whenBucketIsEmpty(String bucket) {
      fixture.setUrl("url");
      fixture.setToken("token");
      fixture.setOrg("org");

      fixture.setBucket(bucket);

      assertThatConnectionExceptionIsBeingThrown();
    }

    private void assertThatConnectionExceptionIsBeingThrown() {
      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "InfluxDB connection not properly configured! Please read the docs."
        );
    }
  }
}

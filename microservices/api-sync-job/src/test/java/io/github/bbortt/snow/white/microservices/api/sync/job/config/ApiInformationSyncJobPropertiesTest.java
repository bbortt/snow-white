/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.InitializingBean;

class ApiInformationSyncJobPropertiesTest {

  private ApiSyncJobProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiSyncJobProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }

  @Nested
  class AfterPropertiesSet {

    public static Stream<String> emptyAndNullString() {
      return Stream.of("", null);
    }

    @Test
    void shouldPass_whenBothPropertiesAreSet() {
      fixture.getServiceInterface().setBaseUrl("http://localhost:8080");
      fixture.getServiceInterface().setIndexUri("/api/index");

      assertDoesNotThrow(() -> fixture.afterPropertiesSet());

      assertThat(fixture).satisfies(
        f ->
          assertThat(f.getServiceInterface().getBaseUrl()).isEqualTo(
            "http://localhost:8080"
          ),
        f ->
          assertThat(f.getServiceInterface().getIndexUri()).isEqualTo(
            "/api/index"
          )
      );
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldPass_whenBaseUrlIsEmptyOrNull(String baseUrl) {
      fixture.getServiceInterface().setBaseUrl(baseUrl);
      fixture.getServiceInterface().setIndexUri("/api/index");

      assertDoesNotThrow(() -> fixture.afterPropertiesSet());
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrow_whenIndexUriIsEmptyOrNull(String indexUrl) {
      fixture.getServiceInterface().setBaseUrl("http://localhost:8080");
      fixture.getServiceInterface().setIndexUri(indexUrl);

      assertThatThrownBy(() -> fixture.afterPropertiesSet())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(PREFIX, "base-url", PREFIX, "index-uri");
    }
  }
}

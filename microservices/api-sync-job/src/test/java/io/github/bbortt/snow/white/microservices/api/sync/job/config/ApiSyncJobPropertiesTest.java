/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.InitializingBean;

class ApiSyncJobPropertiesTest {

  protected static Stream<String> emptyAndNullString() {
    return Stream.of("", null);
  }

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
  class AfterPropertiesSetTest {

    @Nested
    class ApiIndexPropertiesTest {

      static Stream<String> emptyAndNullString() {
        return ApiSyncJobPropertiesTest.emptyAndNullString();
      }

      @BeforeEach
      void beforeEachSetup() {
        fixture.getArtifactory().setBaseUrl("baseUrl");
        fixture.getArtifactory().setAccessToken("accessToken");
        fixture.getArtifactory().setRepository("repository");
      }

      @Test
      void shouldPass_whenBaseUrlIsSet() {
        fixture.getApiIndex().setBaseUrl("api-index");

        assertThatCode(() ->
          fixture.afterPropertiesSet()
        ).doesNotThrowAnyException();
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowException_whenBaseUrlIsEmptyOrNull(String baseUrl) {
        fixture.getApiIndex().setBaseUrl(baseUrl);

        assertThatThrownBy(() -> fixture.afterPropertiesSet())
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
            "All properties must be configured - missing: [snow.white.api.sync.job.api-index.base-url]."
          );
      }
    }

    @Nested
    class ArtifactoryPropertiesTest {

      public static Stream<String> emptyAndNullString() {
        return ApiSyncJobPropertiesTest.emptyAndNullString();
      }

      @BeforeEach
      void beforeEachSetup() {
        fixture.getApiIndex().setBaseUrl("api-index");
      }

      @Test
      void shouldPass_whenAllPropertiesAreSet() {
        fixture.getArtifactory().setBaseUrl("baseUrl");
        fixture.getArtifactory().setAccessToken("accessToken");
        fixture.getArtifactory().setRepository("repository");

        assertThatNoException().isThrownBy(() -> fixture.afterPropertiesSet());
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowException_whenBaseUrlIsEmptyOrNull(String baseUrl) {
        fixture.getArtifactory().setBaseUrl(baseUrl);
        fixture.getArtifactory().setAccessToken("accessToken");
        fixture.getArtifactory().setRepository("repository");

        assertThatThrownBy(() -> fixture.afterPropertiesSet()).hasMessage(
          "All properties must be configured - missing: [snow.white.api.sync.job.artifactory.base-url]."
        );
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowException_whenAccessTokenIsEmptyOrNull(
        String accessToken
      ) {
        fixture.getArtifactory().setBaseUrl("baseUrl");
        fixture.getArtifactory().setAccessToken(accessToken);
        fixture.getArtifactory().setRepository("repository");

        assertThatThrownBy(() -> fixture.afterPropertiesSet()).hasMessage(
          "All properties must be configured - missing: [snow.white.api.sync.job.artifactory.access-token]."
        );
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowException_whenRepositoryIsEmptyOrNull(String repository) {
        fixture.getArtifactory().setBaseUrl("baseUrl");
        fixture.getArtifactory().setAccessToken("accessToken");
        fixture.getArtifactory().setRepository(repository);

        assertThatThrownBy(() -> fixture.afterPropertiesSet()).hasMessage(
          "All properties must be configured - missing: [snow.white.api.sync.job.artifactory.repository]."
        );
      }
    }
  }
}

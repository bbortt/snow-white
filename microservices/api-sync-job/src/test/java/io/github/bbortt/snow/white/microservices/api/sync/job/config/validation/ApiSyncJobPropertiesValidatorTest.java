/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.config.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ApiSyncJobPropertiesValidatorTest {

  protected static Stream<String> emptyAndNullString() {
    return Stream.of("", null);
  }

  private ApiSyncJobProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new ApiSyncJobProperties();
  }

  @Nested
  class AfterPropertiesSetTest {

    @Nested
    class ApiIndexPropertiesTest {

      static Stream<String> emptyAndNullString() {
        return ApiSyncJobPropertiesValidatorTest.emptyAndNullString();
      }

      @BeforeEach
      void beforeEachSetup() {
        fixture.getArtifactory().setBaseUrl("baseUrl");
        fixture.getArtifactory().setRepository("repository");
      }

      @Test
      void shouldPass_whenBaseUrlIsSet() {
        fixture.getApiIndex().setBaseUrl("api-index");

        assertThatCode(() ->
          new ApiSyncJobPropertiesValidator(fixture)
        ).doesNotThrowAnyException();
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowException_whenBaseUrlIsEmptyOrNull(String baseUrl) {
        fixture.getApiIndex().setBaseUrl(baseUrl);

        assertThatThrownBy(() -> new ApiSyncJobPropertiesValidator(fixture))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage(
            "All properties must be configured - missing: [snow.white.api.sync.job.api-index.base-url]."
          );
      }
    }

    @Nested
    class ArtifactoryPropertiesTest {

      public static Stream<String> emptyAndNullString() {
        return ApiSyncJobPropertiesValidatorTest.emptyAndNullString();
      }

      @BeforeEach
      void beforeEachSetup() {
        fixture.getApiIndex().setBaseUrl("api-index");
      }

      @Test
      void shouldPass_whenAllPropertiesAreSet() {
        fixture.getArtifactory().setBaseUrl("baseUrl");
        fixture.getArtifactory().setRepository("repository");

        assertThatNoException().isThrownBy(() ->
          new ApiSyncJobPropertiesValidator(fixture)
        );
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowException_whenBaseUrlIsEmptyOrNull(String baseUrl) {
        fixture.getArtifactory().setBaseUrl(baseUrl);
        fixture.getArtifactory().setRepository("repository");

        assertThatThrownBy(() ->
          new ApiSyncJobPropertiesValidator(fixture)
        ).hasMessage(
          "All properties must be configured - missing: [snow.white.api.sync.job.artifactory.base-url]."
        );
      }

      @ParameterizedTest
      @MethodSource("emptyAndNullString")
      void shouldThrowException_whenRepositoryIsEmptyOrNull(String repository) {
        fixture.getArtifactory().setBaseUrl("baseUrl");
        fixture.getArtifactory().setRepository(repository);

        assertThatThrownBy(() ->
          new ApiSyncJobPropertiesValidator(fixture)
        ).hasMessage(
          "All properties must be configured - missing: [snow.white.api.sync.job.artifactory.repository]."
        );
      }
    }
  }
}

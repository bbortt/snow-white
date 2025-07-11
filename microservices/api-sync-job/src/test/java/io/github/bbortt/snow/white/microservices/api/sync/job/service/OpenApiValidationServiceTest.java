/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.NO_SOURCE;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.GRACEFUL;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OpenApiValidationServiceTest {

  private static final String API_TITLE = "Mostly Harmless";

  private OpenApiValidationService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiValidationService();
  }

  @Nested
  class ValidateApiInformationInformationFromIndex {

    @Test
    void ignoresApiWithoutSourceUrl_inGracefulMode() {
      var api = new ApiInformation().withTitle(API_TITLE).withSourceUrl(null);

      ApiInformation resultingApi = fixture.validateApiInformationFromIndex(
        api,
        GRACEFUL
      );

      assertThat(resultingApi.getLoadStatus()).isEqualTo(NO_SOURCE);
    }

    @Test
    void throwsExceptionWithoutSourceUrl_inStrictMode() {
      var api = new ApiInformation().withTitle(API_TITLE).withSourceUrl(null);

      assertThatThrownBy(() ->
        fixture.validateApiInformationFromIndex(api, STRICT)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Encountered API in index without source URL: Mostly Harmless!"
        );
    }

    @Test
    void ignoresApiWithoutApiType_inGracefulMode() {
      var api = new ApiInformation()
        .withTitle(API_TITLE)
        .withSourceUrl("http://localhost:8080/petstore.yml")
        .withApiType(null);

      var resultingApi = fixture.validateApiInformationFromIndex(api, GRACEFUL);

      assertThat(resultingApi.getLoadStatus()).isEqualTo(LOAD_FAILED);
    }

    @Test
    void throwsExceptionWithoutApiType_inStrictMode() {
      var api = new ApiInformation()
        .withTitle(API_TITLE)
        .withSourceUrl("http://localhost:8080/petstore.yml")
        .withApiType(null);

      assertThatThrownBy(() ->
        fixture.validateApiInformationFromIndex(api, STRICT)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Encountered API in index without type definition: Mostly Harmless!"
        );
    }
  }
}

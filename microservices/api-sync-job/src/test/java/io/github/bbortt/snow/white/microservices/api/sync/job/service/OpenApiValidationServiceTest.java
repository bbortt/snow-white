/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.service;

import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.LOAD_FAILED;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.MANDATORY_INFORMATION_MISSING;
import static io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiLoadStatus.NO_SOURCE;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.GRACEFUL;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.STRICT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.api.sync.job.domain.model.ApiInformation;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class OpenApiValidationServiceTest {

  private static final String SERVICE_NAME = "BBC Radio 4";
  private static final String API_NAME = "The Hitchhiker's Guide to the Galaxy";

  private static final String API_TITLE = "Mostly Harmless";

  private OpenApiValidationService fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new OpenApiValidationService();
  }

  @Nested
  class ValidateApiInformationInformationFromIndex {

    public static Stream<ApiInformation> apiInformationWithoutMandatoryField() {
      return Stream.of(
        ApiInformation.builder().title(API_TITLE).build(),
        ApiInformation.builder()
          .title(API_TITLE)
          .serviceName(SERVICE_NAME)
          .build(),
        ApiInformation.builder()
          .title(API_TITLE)
          .serviceName(SERVICE_NAME)
          .name(API_NAME)
          .build()
      );
    }

    @ParameterizedTest
    @MethodSource("apiInformationWithoutMandatoryField")
    void ignoresApiWithoutMandatoryField_inGracefulMode(
      ApiInformation apiInformationWithoutMandatoryField
    ) {
      ApiInformation resultingApi = fixture.validateApiInformationFromIndex(
        apiInformationWithoutMandatoryField,
        GRACEFUL
      );

      assertThat(resultingApi.getLoadStatus()).isEqualTo(
        MANDATORY_INFORMATION_MISSING
      );
    }

    @ParameterizedTest
    @MethodSource("apiInformationWithoutMandatoryField")
    void throwsExceptionWithoutMandatoryField_inStrictMode(
      ApiInformation apiInformationWithoutMandatoryField
    ) {
      assertThatThrownBy(() ->
        fixture.validateApiInformationFromIndex(
          apiInformationWithoutMandatoryField,
          STRICT
        )
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Encountered API in index mandatory fields service name, API name and version: Mostly Harmless!"
        );
    }

    @Test
    void ignoresApiWithoutSourceUrl_inGracefulMode() {
      var api = getApiInformationWithMandatoryProperties().withSourceUrl(null);

      ApiInformation resultingApi = fixture.validateApiInformationFromIndex(
        api,
        GRACEFUL
      );

      assertThat(resultingApi.getLoadStatus()).isEqualTo(NO_SOURCE);
    }

    @Test
    void throwsExceptionWithoutSourceUrl_inStrictMode() {
      var api = getApiInformationWithMandatoryProperties().withSourceUrl(null);

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
      var api = getApiInformationWithMandatoryProperties()
        .withSourceUrl("http://localhost:3000/petstore.yml")
        .withApiType(null);

      var resultingApi = fixture.validateApiInformationFromIndex(api, GRACEFUL);

      assertThat(resultingApi.getLoadStatus()).isEqualTo(LOAD_FAILED);
    }

    @Test
    void throwsExceptionWithoutApiType_inStrictMode() {
      var api = getApiInformationWithMandatoryProperties()
        .withSourceUrl("http://localhost:3000/petstore.yml")
        .withApiType(null);

      assertThatThrownBy(() ->
        fixture.validateApiInformationFromIndex(api, STRICT)
      )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
          "Encountered API in index without type definition: Mostly Harmless!"
        );
    }

    private ApiInformation getApiInformationWithMandatoryProperties() {
      return ApiInformation.builder()
        .title(API_TITLE)
        .serviceName(SERVICE_NAME)
        .name(API_NAME)
        .version("1.2.3")
        .build();
    }
  }
}

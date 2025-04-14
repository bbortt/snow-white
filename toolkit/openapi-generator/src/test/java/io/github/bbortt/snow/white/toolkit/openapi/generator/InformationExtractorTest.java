/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InformationExtractorTest {

  private static final String API_NAME_PATH = "info.title";
  private static final String API_VERSION_PATH = "info.version";
  private static final String SERVICE_NAME_PATH = "info.x-service-name";

  private static final String VALID_API_NAME = "Test API";
  private static final String VALID_API_VERSION = "1.0.0";
  private static final String VALID_SERVICE_NAME = "test-service";

  private InformationExtractor fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new InformationExtractor(
      API_NAME_PATH,
      API_VERSION_PATH,
      SERVICE_NAME_PATH
    );
  }

  @Nested
  class ExtractFromOpenApi {

    @Test
    void returnsCompleteInformationWithAllFieldsPresent() {
      String openApi = // language=json
        """
        {
            "info": {
                "title": "%s",
                "version": "%s",
                "x-service-name": "%s"
            }
        }""".formatted(VALID_API_NAME, VALID_API_VERSION, VALID_SERVICE_NAME);

      OpenApiInformation result = fixture.extractFromOpenApi(openApi);

      assertThat(result).satisfies(
        r -> assertThat(result.apiName()).isEqualTo(VALID_API_NAME),
        r -> assertThat(result.apiVersion()).isEqualTo(VALID_API_VERSION),
        r -> assertThat(result.serviceName()).isEqualTo(VALID_SERVICE_NAME),
        r -> assertThat(result.isIncomplete()).isFalse()
      );
    }

    @Test
    void returnsIncompleteInformationWithMissingFields() {
      String openApi = // language=json
        """
        {
            "info": {
                "title": "%s"
            }
        }""".formatted(VALID_API_NAME);

      OpenApiInformation result = fixture.extractFromOpenApi(openApi);

      assertThat(result).satisfies(
        r -> assertThat(result.apiName()).isEqualTo(VALID_API_NAME),
        r -> assertThat(result.apiVersion()).isNull(),
        r -> assertThat(result.serviceName()).isNull(),
        r -> assertThat(result.isIncomplete()).isTrue()
      );
    }

    @Test
    void returnsAllNullValuesWithEmptyObject() {
      String openApi = // language=json
        "{}";

      OpenApiInformation result = fixture.extractFromOpenApi(openApi);

      assertThat(result).satisfies(
        r -> assertThat(result.apiName()).isNull(),
        r -> assertThat(result.apiVersion()).isNull(),
        r -> assertThat(result.serviceName()).isNull(),
        r -> assertThat(result.isIncomplete()).isTrue()
      );
    }

    @Test
    void extractsFromDifferentJsonStructure() {
      fixture = new InformationExtractor(
        "customInfo.apiName",
        "customInfo.apiVersion",
        "customInfo.serviceName"
      );

      var openApi = // language=json
        """
        {
            "customInfo": {
                "apiName": "%s",
                "apiVersion": "%s",
                "serviceName": "%s"
            }
        }""".formatted(VALID_API_NAME, VALID_API_VERSION, VALID_SERVICE_NAME);

      OpenApiInformation result = fixture.extractFromOpenApi(openApi);

      assertThat(result).satisfies(
        r -> assertThat(result.apiName()).isEqualTo(VALID_API_NAME),
        r -> assertThat(result.apiVersion()).isEqualTo(VALID_API_VERSION),
        r -> assertThat(result.serviceName()).isEqualTo(VALID_SERVICE_NAME),
        r -> assertThat(result.isIncomplete()).isFalse()
      );
    }

    @Test
    void extractsNestedPaths() {
      fixture = new InformationExtractor(
        "deep.nested.apiName",
        "deep.nested.apiVersion",
        "deep.nested.serviceName"
      );

      var openApi = // language=json
        """
        {
            "deep": {
                "nested": {
                    "apiName": "%s",
                    "apiVersion": "%s",
                    "serviceName": "%s"
                }
            }
        }""".formatted(VALID_API_NAME, VALID_API_VERSION, VALID_SERVICE_NAME);

      OpenApiInformation result = fixture.extractFromOpenApi(openApi);

      assertThat(result).satisfies(
        r -> assertThat(result.apiName()).isEqualTo(VALID_API_NAME),
        r -> assertThat(result.apiVersion()).isEqualTo(VALID_API_VERSION),
        r -> assertThat(result.serviceName()).isEqualTo(VALID_SERVICE_NAME),
        r -> assertThat(result.isIncomplete()).isFalse()
      );
    }

    @Test
    void returnsAllNullValuesWithInvalidJson() {
      var openApi = "invalid json";

      OpenApiInformation result = fixture.extractFromOpenApi(openApi);

      assertThat(result).satisfies(
        r -> assertThat(r.apiName()).isNull(),
        r -> assertThat(r.apiVersion()).isNull(),
        r -> assertThat(r.serviceName()).isNull(),
        r -> assertThat(r.isIncomplete()).isTrue()
      );
    }
  }
}

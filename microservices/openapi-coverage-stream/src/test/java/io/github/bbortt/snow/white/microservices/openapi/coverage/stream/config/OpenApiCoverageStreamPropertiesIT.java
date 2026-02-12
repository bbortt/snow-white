/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.AbstractOpenApiCoverageServiceIT;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

class OpenApiCoverageStreamPropertiesIT
  extends AbstractOpenApiCoverageServiceIT
{

  @Autowired
  private OpenApiCoverageStreamProperties openApiCoverageStreamProperties;

  @Nested
  @TestPropertySource(
    locations = {
      "classpath:/OpenApiCoverageStreamPropertiesIT/application.properties",
    }
  )
  class FilteringPropertiesIT {

    @Test
    void shouldHavePropertiesFromValues() {
      assertThat(openApiCoverageStreamProperties.getFiltering())
        .isNotNull()
        .satisfies(
          f ->
            assertThat(f.getApiNameAttributeKey()).isEqualTo("custom-api-name"),
          f ->
            assertThat(f.getApiVersionAttributeKey()).isEqualTo(
              "custom-api-version"
            ),
          f ->
            assertThat(f.getServiceNameAttributeKey()).isEqualTo(
              "custom-service-name"
            )
        );
    }
  }
}

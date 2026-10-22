/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThat;

import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesSw;
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

  /**
   * The upstream convention is the default, so a suite already emitting {@code test.case.name}
   * needs no configuration at all.
   */
  @Test
  @VerifiesSw(SwTraceables.SW_032_TEST_IDENTITY_ON_THE_SPAN)
  void shouldDefaultTheTestIdentityAttributeToTheUpstreamConvention() {
    assertThat(
      openApiCoverageStreamProperties.getTestCaseNameAttribute()
    ).isEqualTo("test.case.name");
  }

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

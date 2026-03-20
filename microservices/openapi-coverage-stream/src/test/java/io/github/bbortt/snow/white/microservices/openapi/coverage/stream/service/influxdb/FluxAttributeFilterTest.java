/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.service.influxdb;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FluxAttributeFilterTest {

  private static final String KEY = "http.target";
  private static final AttributeFilter BASE_ATTRIBUTE_FILTER =
    new AttributeFilter(
      KEY,
      AttributeFilterOperator.STRING_EQUALS,
      "/api/v1/users"
    );

  private FluxAttributeFilter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new FluxAttributeFilter(BASE_ATTRIBUTE_FILTER);
  }

  @Nested
  class GetBaseAttributeFilterTest {

    @Test
    void shouldReturnBaseAttributeFilter() {
      assertThat(fixture.getBaseAttributeFilter()).isEqualTo(
        BASE_ATTRIBUTE_FILTER
      );
    }
  }

  @Nested
  class GetKeyTest {

    @Test
    void shouldReturnKeyOfBaseAttributeFilter() {
      assertThat(fixture.getKey()).isEqualTo(KEY);
    }
  }

  @Nested
  class GetNormalizedKeyTest {

    @Test
    void shouldReturnNormalizedKey() {
      assertThat(fixture.getNormalizedKey()).isEqualTo("http_target");
    }
  }

  @Nested
  class ToFluxStringTest {

    @Test
    void shouldGenerateCorrectFluxStringFormat_whenUsingStringEquals() {
      String fluxString = fixture.toFluxString();

      assertThat(fluxString).isEqualTo(
        " |> filter(fn: (r) => r.http_target == \"/api/v1/users\")"
      );
    }
  }
}

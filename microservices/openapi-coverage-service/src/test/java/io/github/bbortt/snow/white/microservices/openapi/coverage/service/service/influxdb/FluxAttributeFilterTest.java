/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb;

import static io.github.bbortt.snow.white.commons.event.dto.AttributeFilterOperator.STRING_EQUALS;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.commons.event.dto.AttributeFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FluxAttributeFilterTest {

  private static final String KEY = "http.target";

  private FluxAttributeFilter fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new FluxAttributeFilter(
      new AttributeFilter(KEY, STRING_EQUALS, "/api/v1/users")
    );
  }

  @Nested
  class GetKey {

    @Test
    void shouldReturnKeyOfBaseAttributeFilter() {
      assertThat(fixture.getKey()).isEqualTo(KEY);
    }
  }

  @Nested
  class GetNormalizedKey {

    @Test
    void shouldReturnNormalizedKey() {
      assertThat(fixture.getNormalizedKey()).isEqualTo("http_target");
    }
  }

  @Nested
  class ToFluxString {

    @Test
    void shouldGenerateCorrectFluxStringFormat_whenUsingStringEquals() {
      String fluxString = fixture.toFluxString();

      assertThat(fluxString).isEqualTo(
        " |> filter(fn: (r) => r.http_target == \"/api/v1/users\")"
      );
    }
  }
}

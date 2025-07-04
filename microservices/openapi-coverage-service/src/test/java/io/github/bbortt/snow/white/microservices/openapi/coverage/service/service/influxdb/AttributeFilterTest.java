/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.influxdb.AttributeFilterOperator.STRING_EQUALS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AttributeFilterTest {

  @Nested
  class ToFluxString {

    @Test
    void shouldGenerateCorrectFluxStringFormat_whenUsingStringEquals() {
      var filter = new AttributeFilter(
        "http.target",
        STRING_EQUALS,
        "/api/v1/users"
      );

      String fluxString = filter.toFluxString();

      assertThat(fluxString).isEqualTo(
        "  |> filter(fn: (r) => json.parse(v: r._value)[\"[\"http.target\"]\"] == \"/api/v1/users\") "
      );
    }
  }

  @Nested
  class AttributeFilters {

    @Test
    void shouldReturnBuilder() {
      AttributeFilter.Builder builder = AttributeFilter.attributeFilters();

      assertThat(builder)
        .isNotNull()
        .isInstanceOf(AttributeFilter.Builder.class);
    }
  }

  @Nested
  class Builder {

    @Test
    void shouldReturnEmptyList_whenNoFiltersAdded() {
      List<AttributeFilter> filters =
        AttributeFilter.attributeFilters().build();

      assertThat(filters).isEmpty();
    }

    @Nested
    class With {

      @Test
      void shouldAddAttributeFilterToList() {
        var filter1 = new AttributeFilter("http.method", STRING_EQUALS, "GET");
        var filter2 = new AttributeFilter(
          "http.target",
          STRING_EQUALS,
          "/api/v1/users"
        );

        List<AttributeFilter> filters = AttributeFilter.attributeFilters()
          .with(filter1)
          .with(filter2)
          .build();

        assertThat(filters).containsExactly(filter1, filter2);
      }
    }
  }
}

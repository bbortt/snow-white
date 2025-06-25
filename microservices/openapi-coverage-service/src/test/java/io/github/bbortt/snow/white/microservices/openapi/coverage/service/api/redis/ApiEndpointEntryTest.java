/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.redis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisHash;

class ApiEndpointEntryTest {

  private static final String ID = "test-id";
  private static final String OTEL_SERVICE_NAME = "test-service";
  private static final String API_NAME = "test-api";
  private static final String API_VERSION = "v1";
  private static final String SOURCE_URL = "https://example.com/openapi.json";

  @Nested
  class Equals {

    @Test
    void shouldReturnTrueForSameId() {
      ApiEndpointEntry entry1 = ApiEndpointEntry.builder().id(ID).build();
      ApiEndpointEntry entry2 = ApiEndpointEntry.builder().id(ID).build();

      assertThat(entry1).isEqualTo(entry2);
    }

    @Test
    void shouldReturnFalseForDifferentId() {
      ApiEndpointEntry entry1 = ApiEndpointEntry.builder().id(ID).build();
      ApiEndpointEntry entry2 = ApiEndpointEntry.builder()
        .id("different-id")
        .build();

      assertThat(entry1).isNotEqualTo(entry2);
    }

    @Test
    void shouldReturnFalseForNullId() {
      ApiEndpointEntry entry1 = ApiEndpointEntry.builder().id(null).build();
      ApiEndpointEntry entry2 = ApiEndpointEntry.builder().id(ID).build();

      assertThat(entry1).isNotEqualTo(entry2);
    }

    @Test
    void equalsShouldReturnFalseForDifferentObjectType() {
      ApiEndpointEntry entry = ApiEndpointEntry.builder().id(ID).build();

      assertThat(entry).isNotEqualTo("not an ApiEndpointEntry");
    }
  }

  @Nested
  class HashCode {

    @Test
    void shouldBeConsistentWithEquals() {
      ApiEndpointEntry entry1 = ApiEndpointEntry.builder().id(ID).build();
      ApiEndpointEntry entry2 = ApiEndpointEntry.builder().id(ID).build();

      assertThat(entry1).hasSameHashCodeAs(entry2);
    }

    @Test
    void shouldBeDifferentForDifferentIds() {
      ApiEndpointEntry entry1 = ApiEndpointEntry.builder().id(ID).build();
      ApiEndpointEntry entry2 = ApiEndpointEntry.builder()
        .id("different-id")
        .build();

      assertThat(entry1.hashCode()).isNotEqualTo(entry2.hashCode());
    }
  }

  @Test
  @DisplayName("RedisHash annotation value should be 'api_endpoints'")
  void redisHashAnnotationValueShouldBeApiEndpoints() {
    Class<ApiEndpointEntry> clazz = ApiEndpointEntry.class;
    var redisHashAnnotation = clazz.getAnnotation(RedisHash.class);

    assertThat(redisHashAnnotation)
      .isNotNull()
      .extracting(RedisHash::value)
      .isEqualTo("api_endpoints");
  }
}

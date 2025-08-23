/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.commons.redis;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.ReflectionUtils.setField;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisHash;

class ApiEndpointEntryTest {

  private static final String ID = "test-id";
  private static final String SERVICE_NAME = "test-service";
  private static final String API_NAME = "test-api";
  private static final String API_VERSION = "v1";
  private static final String SOURCE_URL = "https://example.com/openapi.json";

  private static ApiEndpointEntry fakeApiEndpointEntry(String id) {
    return new ApiEndpointEntry(
      SERVICE_NAME,
      API_NAME,
      API_VERSION,
      SOURCE_URL,
      OPENAPI
    ).withId(id);
  }

  @Nested
  class Equals {

    @Test
    void shouldReturnTrueForSameId() {
      ApiEndpointEntry entry1 = fakeApiEndpointEntry(ID);
      ApiEndpointEntry entry2 = fakeApiEndpointEntry(ID);

      assertThat(entry1).isEqualTo(entry2);
    }

    @Test
    void shouldReturnFalseForDifferentId() {
      ApiEndpointEntry entry1 = fakeApiEndpointEntry(ID);
      ApiEndpointEntry entry2 = fakeApiEndpointEntry("different-id");

      assertThat(entry1).isNotEqualTo(entry2);
    }

    @Test
    void shouldReturnFalseForNullId() throws NoSuchFieldException {
      ApiEndpointEntry entry1 = fakeApiEndpointEntry(ID);
      var idField = ApiEndpointEntry.class.getDeclaredField("id");
      idField.setAccessible(true);
      setField(idField, entry1, null);

      ApiEndpointEntry entry2 = fakeApiEndpointEntry(ID);

      assertThat(entry1).isNotEqualTo(entry2);
    }

    @Test
    void equalsShouldReturnFalseForDifferentObjectType() {
      ApiEndpointEntry entry = fakeApiEndpointEntry(ID);

      assertThat(entry).isNotEqualTo("not an ApiEndpointEntry");
    }
  }

  @Nested
  class HashCode {

    @Test
    void shouldBeConsistentWithEquals() {
      ApiEndpointEntry entry1 = fakeApiEndpointEntry(ID);
      ApiEndpointEntry entry2 = fakeApiEndpointEntry(ID);

      assertThat(entry1).hasSameHashCodeAs(entry2);
    }

    @Test
    void shouldBeDifferentForDifferentIds() {
      ApiEndpointEntry entry1 = fakeApiEndpointEntry(ID);
      ApiEndpointEntry entry2 = fakeApiEndpointEntry("different-id");

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

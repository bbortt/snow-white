/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job.api.redis;

import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.ASYNCAPI;
import static io.github.bbortt.snow.white.commons.quality.gate.ApiType.OPENAPI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.bbortt.snow.white.commons.redis.ApiEndpointEntry;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApiInformationEndpointEntryTest {

  @Nested
  class Constructor {

    @Test
    void shouldCreateEntryWithValidParameters() {
      var serviceName = "test-service";
      var apiName = "test-api";
      var apiVersion = "1.2.3";
      var sourceUrl = "https://sample.repository";

      var fixture = new ApiEndpointEntry(
        serviceName,
        apiName,
        apiVersion,
        sourceUrl,
        OPENAPI
      );

      assertThat(fixture).satisfies(
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getId()).isEqualTo(
            "test-service:test-api:1.2.3"
          ),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getOtelServiceName()).isEqualTo(
            serviceName
          ),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getApiName()).isEqualTo(apiName),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getApiVersion()).isEqualTo(apiVersion),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getSourceUrl()).isEqualTo(sourceUrl),
        apiEndpointEntry ->
          assertThat(apiEndpointEntry.getApiType()).isEqualTo(OPENAPI.getVal())
      );
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void shouldBeEqualForSameId() {
      var otelServiceName = "service";
      var apiName = "api";
      var apiVersion = "1.2.3";

      var entry1 = new ApiEndpointEntry(
        otelServiceName,
        apiName,
        apiVersion,
        "foo",
        ASYNCAPI
      );
      var entry2 = new ApiEndpointEntry(
        otelServiceName,
        apiName,
        apiVersion,
        "bar",
        OPENAPI
      );

      assertThat(entry1).isEqualTo(entry2).hasSameHashCodeAs(entry2);
    }

    public static Stream<Arguments> idCombinations() {
      return Stream.of(
        arguments(
          "service-1",
          "api-1",
          "1.2.3",
          // otelServiceName differs
          "service-2",
          "api-1",
          "1.2.3"
        ),
        arguments(
          "service-1",
          "api-1",
          "1.2.3",
          // apiName differs
          "service-1",
          "api-2",
          "1.2.3"
        ),
        arguments(
          "service-1",
          "api-1",
          "1.2.3",
          // apiVersion differs
          "service-1",
          "api-2",
          "2.3.4"
        )
      );
    }

    @ParameterizedTest
    @MethodSource("idCombinations")
    void shouldNotBeEqualForDifferentIdCombinations(
      String otelServiceName1,
      String apiName1,
      String apiVersion1,
      String otelServiceName2,
      String apiName2,
      String apiVersion2
    ) {
      var entry1 = new ApiEndpointEntry(
        otelServiceName1,
        apiName1,
        apiVersion1,
        "foo",
        ASYNCAPI
      );
      var entry2 = new ApiEndpointEntry(
        otelServiceName2,
        apiName2,
        apiVersion2,
        "foo",
        OPENAPI
      );

      assertThat(entry1).isNotEqualTo(entry2).doesNotHaveSameHashCodeAs(entry2);
    }
  }
}

package io.github.bbortt.snow.white.api.sync.job.storage.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ApiEndpointEntryTest {

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
        sourceUrl
      );

      assertThat(fixture).satisfies(
        f -> assertThat(f.getId()).isEqualTo("test-service:test-api:1.2.3"),
        f -> assertThat(f.getOtelServiceName()).isEqualTo(serviceName),
        f -> assertThat(f.getApiName()).isEqualTo(apiName),
        f -> assertThat(f.getApiVersion()).isEqualTo(apiVersion),
        f -> assertThat(f.getSourceUrl()).isEqualTo(sourceUrl)
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
        "foo"
      );
      var entry2 = new ApiEndpointEntry(
        otelServiceName,
        apiName,
        apiVersion,
        "bar"
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
        "foo"
      );
      var entry2 = new ApiEndpointEntry(
        otelServiceName2,
        apiName2,
        apiVersion2,
        "foo"
      );

      assertThat(entry1).isNotEqualTo(entry2).doesNotHaveSameHashCodeAs(entry2);
    }
  }
}

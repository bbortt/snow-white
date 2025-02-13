package io.github.bbortt.snow.white.toolkit.openapi.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OpenApiInformationTest {

  private static final String API_NAME = "test-api";
  private static final String API_VERSION = "v1.0.0";
  private static final String SERVICE_NAME = "test-service";

  @Test
  void constructorCreatesInstance() {
    var openApiInformation = new OpenApiInformation(
      API_NAME,
      API_VERSION,
      SERVICE_NAME
    );

    assertThat(openApiInformation.apiName()).isEqualTo(API_NAME);
    assertThat(openApiInformation.apiVersion()).isEqualTo(API_VERSION);
    assertThat(openApiInformation.serviceName()).isEqualTo(SERVICE_NAME);
    assertThat(openApiInformation.isIncomplete()).isFalse();
  }

  @Nested
  class IsIncomplete {

    @Test
    void withAllValidValuesReturnsFalse() {
      var openApiInformation = new OpenApiInformation(
        API_NAME,
        API_VERSION,
        SERVICE_NAME
      );

      assertThat(openApiInformation.isIncomplete()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("provideIncompleteValues")
    void withMissingOrBlankValuesReturnsTrue(
      String apiName,
      String apiVersion,
      String serviceName
    ) {
      var openApiInformation = new OpenApiInformation(
        apiName,
        apiVersion,
        serviceName
      );

      assertThat(openApiInformation.isIncomplete()).isTrue();
    }

    private static Stream<Arguments> provideIncompleteValues() {
      return Stream.of(
        // Null values
        arguments(null, API_VERSION, SERVICE_NAME),
        arguments(API_NAME, null, SERVICE_NAME),
        arguments(API_NAME, API_VERSION, null),
        // Empty strings
        arguments("", API_VERSION, SERVICE_NAME),
        arguments(API_NAME, "", SERVICE_NAME),
        arguments(API_NAME, API_VERSION, ""),
        // Blank strings
        arguments("   ", API_VERSION, SERVICE_NAME),
        arguments(API_NAME, "   ", SERVICE_NAME),
        arguments(API_NAME, API_VERSION, "   "),
        // Multiple blank/null combinations
        arguments(null, null, SERVICE_NAME),
        arguments(null, API_VERSION, null),
        arguments("", "   ", SERVICE_NAME),
        arguments(API_NAME, "", "   ")
      );
    }
  }
}

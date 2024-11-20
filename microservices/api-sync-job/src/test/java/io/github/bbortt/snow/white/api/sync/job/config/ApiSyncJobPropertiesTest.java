package io.github.bbortt.snow.white.api.sync.job.config;

import static io.github.bbortt.snow.white.api.sync.job.config.ApiSyncJobProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ApiSyncJobPropertiesTest {

  private ApiSyncJobProperties fixture;

  @BeforeEach
  void setUp() {
    fixture = new ApiSyncJobProperties();
  }

  @Nested
  class SanitizeProperties {

    public static Stream<String> emptyAndNullString() {
      return Stream.of("", null);
    }

    @Test
    void shouldPassWhenBothPropertiesAreSet() {
      fixture.getServiceInterface().setBaseUrl("http://localhost:8080");
      fixture.getServiceInterface().setIndexUri("/api/index");

      assertDoesNotThrow(() -> fixture.sanitizeProperties());

      assertThat(fixture).satisfies(
        f ->
          assertThat(f.getServiceInterface().getBaseUrl()).isEqualTo(
            "http://localhost:8080"
          ),
        f ->
          assertThat(f.getServiceInterface().getIndexUri()).isEqualTo(
            "/api/index"
          )
      );
    }

    @Test
    void shouldThrowWhenBaseUrlIsNull() {
      fixture.getServiceInterface().setBaseUrl(null);
      fixture.getServiceInterface().setIndexUri("/api/index");

      assertThatThrownBy(() -> fixture.sanitizeProperties())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("base-url")
        .hasMessageContaining("index-uri");
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowWhenBaseUrlIsEmptyOrNull(String baseUrl) {
      fixture.getServiceInterface().setBaseUrl(baseUrl);
      fixture.getServiceInterface().setIndexUri("/api/index");

      assertSanitizePropertiesThrows();
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowWhenIndexUriIsEmptyOrNull(String indexUrl) {
      fixture.getServiceInterface().setBaseUrl("http://localhost:8080");
      fixture.getServiceInterface().setIndexUri(indexUrl);

      assertSanitizePropertiesThrows();
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowWhenBothPropertiesAreEmptyOrNull(String value) {
      fixture.getServiceInterface().setBaseUrl(value);
      fixture.getServiceInterface().setIndexUri(value);

      assertSanitizePropertiesThrows();
    }

    private void assertSanitizePropertiesThrows() {
      assertThatThrownBy(() -> fixture.sanitizeProperties())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(PREFIX, "base-url", PREFIX, "index-uri");
    }
  }
}

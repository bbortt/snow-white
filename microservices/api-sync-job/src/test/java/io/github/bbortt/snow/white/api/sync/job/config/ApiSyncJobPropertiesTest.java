package io.github.bbortt.snow.white.api.sync.job.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
  @DisplayName("sanitizeProperties")
  class SanitizeProperties {

    public static Stream<String> emptyAndNullString() {
      return Stream.of("", null);
    }

    @Test
    void shouldPassWhenBothPropertiesAreSet() {
      fixture.getServiceInterface().setBaseUrl("http://localhost:8080");
      fixture.getServiceInterface().setIndexUri("/api/index");

      fixture.sanitizeProperties();

      assertThat(fixture.getServiceInterface().getBaseUrl()).isEqualTo(
        "http://localhost:8080"
      );
      assertThat(fixture.getServiceInterface().getIndexUri()).isEqualTo(
        "/api/index"
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

      assertThatThrownBy(() -> fixture.sanitizeProperties())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("base-url")
        .hasMessageContaining("index-uri");
    }

    @ParameterizedTest
    @MethodSource("emptyAndNullString")
    void shouldThrowWhenIndexUriIsEmptyOrNull(String indexUrl) {
      fixture.getServiceInterface().setBaseUrl("http://localhost:8080");
      fixture.getServiceInterface().setIndexUri(indexUrl);

      assertThatThrownBy(() -> fixture.sanitizeProperties())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("base-url")
        .hasMessageContaining("index-uri");
    }

    @Test
    @DisplayName(
      "should throw IllegalArgumentException when both properties are null"
    )
    void shouldThrowWhenBothPropertiesAreNull() {
      fixture.getServiceInterface().setBaseUrl(null);
      fixture.getServiceInterface().setIndexUri(null);

      assertThatThrownBy(() -> fixture.sanitizeProperties())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("base-url")
        .hasMessageContaining("index-uri");
    }
  }
}

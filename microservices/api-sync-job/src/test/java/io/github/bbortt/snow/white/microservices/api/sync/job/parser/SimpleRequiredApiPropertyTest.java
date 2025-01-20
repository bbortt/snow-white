package io.github.bbortt.snow.white.microservices.api.sync.job.parser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SimpleRequiredApiPropertyTest {

  @Nested
  class GetPropertyName {

    @Test
    void shouldReturnPropertyName() {
      var propertyName = "test.property";

      var fixture = new SimpleRequiredApiProperty(propertyName);

      assertThat(fixture.getPropertyName())
        .isEqualTo(propertyName)
        .isEqualTo(fixture.propertyName());
    }
  }

  @Nested
  class IsRequired {

    @Test
    void shouldAlwaysReturnTrue() {
      var fixture = new SimpleRequiredApiProperty("test.property");
      assertThat(fixture.isRequired()).isTrue();
    }
  }

  @Nested
  class EqualsAndHashCode {

    @Test
    void shouldBeEqualForSamePropertyName() {
      var propertyName = "test.property";

      var property1 = new SimpleRequiredApiProperty(propertyName);
      var property2 = new SimpleRequiredApiProperty(propertyName);

      assertThat(property1).isEqualTo(property2).hasSameHashCodeAs(property2);
    }

    @Test
    void shouldNotBeEqualForDifferentPropertyNames() {
      var property1 = new SimpleRequiredApiProperty("property1");
      var property2 = new SimpleRequiredApiProperty("property2");

      assertThat(property1)
        .isNotEqualTo(property2)
        .doesNotHaveSameHashCodeAs(property2);
    }
  }
}

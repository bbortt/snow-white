package io.github.bbortt.snow.white.toolkit.spring.web;

import static io.github.bbortt.snow.white.toolkit.spring.web.SnowWhiteAutoConfiguration.PROPERTY_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SnowWhiteAutoConfigurationTest {

  @Test
  void prefixIsPackageName() {
    assertThat(PROPERTY_PREFIX).isEqualTo(
      SnowWhiteAutoConfiguration.class.getPackageName()
    );
  }
}

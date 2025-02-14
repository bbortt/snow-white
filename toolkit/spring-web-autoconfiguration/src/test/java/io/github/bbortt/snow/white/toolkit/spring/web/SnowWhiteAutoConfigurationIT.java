package io.github.bbortt.snow.white.toolkit.spring.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties;
import io.github.bbortt.snow.white.toolkit.spring.web.interceptor.OpenApiInformationEnhancer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SnowWhiteAutoConfiguration.class)
class SnowWhiteAutoConfigurationIT {

  @Autowired
  private OpenApiInformationEnhancer openApiInformationEnhancer;

  @Autowired
  private SpringWebInterceptorProperties springWebInterceptorProperties;

  @Test
  void propertiesAreLoaded() {
    assertThat(springWebInterceptorProperties).isNotNull();
  }

  @Test
  void aspectIsLoaded() {
    assertThat(openApiInformationEnhancer).isNotNull();
  }
}

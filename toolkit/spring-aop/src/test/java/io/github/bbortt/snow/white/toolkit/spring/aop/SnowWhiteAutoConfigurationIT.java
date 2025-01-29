package io.github.bbortt.snow.white.toolkit.spring.aop;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.toolkit.spring.aop.aspect.RestControllerAspect;
import io.github.bbortt.snow.white.toolkit.spring.aop.config.SnowWhiteSpringAopProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = SnowWhiteAutoConfiguration.class)
class SnowWhiteAutoConfigurationIT {

  @Autowired
  private RestControllerAspect restControllerAspect;

  @Autowired
  private SnowWhiteSpringAopProperties snowWhiteSpringAopProperties;

  @Test
  void propertiesAreLoaded() {
    assertThat(snowWhiteSpringAopProperties).isNotNull();
  }

  @Test
  void aspectIsLoaded() {
    assertThat(restControllerAspect).isNotNull();
  }
}

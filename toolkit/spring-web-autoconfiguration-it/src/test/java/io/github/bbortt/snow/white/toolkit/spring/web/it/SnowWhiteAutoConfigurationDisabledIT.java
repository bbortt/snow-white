package io.github.bbortt.snow.white.toolkit.spring.web.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.toolkit.spring.web.SnowWhiteAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

@Isolated
@DirtiesContext
@SpringBootTest(
  properties = {
    "io.github.bbortt.snow.white.toolkit.spring.web.enabled=false",
  }
)
class SnowWhiteAutoConfigurationDisabledIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void autoconfigurationIsEnabled() {
    assertThatThrownBy(() ->
      applicationContext.getBean(SnowWhiteAutoConfiguration.class)
    ).isInstanceOf(NoSuchBeanDefinitionException.class);
  }
}

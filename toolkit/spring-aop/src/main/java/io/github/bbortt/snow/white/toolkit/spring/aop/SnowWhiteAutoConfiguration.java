package io.github.bbortt.snow.white.toolkit.spring.aop;

import io.github.bbortt.snow.white.toolkit.spring.aop.aspect.RestControllerAspect;
import io.github.bbortt.snow.white.toolkit.spring.aop.config.SnowWhiteSpringAopProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@Slf4j
@AutoConfiguration
@Import({ RestControllerAspect.class, SnowWhiteSpringAopProperties.class })
public class SnowWhiteAutoConfiguration {

  public SnowWhiteAutoConfiguration() {
    log.info("Enhancing OTEL Spans with Snow-White information ✅");
  }
}

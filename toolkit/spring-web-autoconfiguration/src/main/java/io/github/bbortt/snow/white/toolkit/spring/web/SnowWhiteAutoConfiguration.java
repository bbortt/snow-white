package io.github.bbortt.snow.white.toolkit.spring.web;

import static io.github.bbortt.snow.white.toolkit.spring.web.SnowWhiteAutoConfiguration.PROPERTY;
import static io.github.bbortt.snow.white.toolkit.spring.web.SnowWhiteAutoConfiguration.PROPERTY_PREFIX;

import io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties;
import io.github.bbortt.snow.white.toolkit.spring.web.interceptor.OpenApiInformationEnhancer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@AutoConfiguration
@Import(
  { SpringWebInterceptorProperties.class, OpenApiInformationEnhancer.class }
)
@ConditionalOnProperty(
  prefix = PROPERTY_PREFIX,
  name = PROPERTY,
  havingValue = "true",
  matchIfMissing = true
)
public class SnowWhiteAutoConfiguration implements WebMvcConfigurer {

  static final String PROPERTY_PREFIX =
    "io.github.bbortt.snow.white.toolkit.spring.web";
  static final String PROPERTY = "enabled";

  private final OpenApiInformationEnhancer openApiInformationEnhancer;

  public SnowWhiteAutoConfiguration(
    OpenApiInformationEnhancer openApiInformationEnhancer
  ) {
    this.openApiInformationEnhancer = openApiInformationEnhancer;
    logger.info("Enhancing OTEL Spans with Snow-White information ✅");
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(openApiInformationEnhancer);
  }
}

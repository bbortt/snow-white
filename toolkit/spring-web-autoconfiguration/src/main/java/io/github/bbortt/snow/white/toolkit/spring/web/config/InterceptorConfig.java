package io.github.bbortt.snow.white.toolkit.spring.web.config;

import io.github.bbortt.snow.white.toolkit.spring.web.interceptor.OpenApiInformationEnhancer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

  private final OpenApiInformationEnhancer openApiInformationEnhancer;

  public InterceptorConfig(
    OpenApiInformationEnhancer openApiInformationEnhancer
  ) {
    this.openApiInformationEnhancer = openApiInformationEnhancer;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(openApiInformationEnhancer);
  }
}

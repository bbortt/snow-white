package io.github.bbortt.snow.white.toolkit.spring.web.config;

import io.github.bbortt.snow.white.toolkit.spring.web.interceptor.OpenApiInformationEnhancer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class InterceptorConfig implements WebMvcConfigurer {

  private final OpenApiInformationEnhancer openApiInformationEnhancer;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(openApiInformationEnhancer);
  }
}

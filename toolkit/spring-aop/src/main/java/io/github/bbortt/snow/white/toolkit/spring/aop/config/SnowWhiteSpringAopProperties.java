package io.github.bbortt.snow.white.toolkit.spring.aop.config;

import static io.github.bbortt.snow.white.toolkit.spring.aop.config.SnowWhiteSpringAopProperties.PREFIX;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class SnowWhiteSpringAopProperties {

  static final String PREFIX = "io.github.bbortt.snow.white.spring.aop";

  private static final String DEFAULT_API_NAME_PROPERTY = "api.name";
  private static final String DEFAULT_API_VERSION_PROPERTY = "api.version";
  private static final String DEFAULT_OTEL_SERVICE_NAME_PROPERTY =
    SERVICE_NAME.getKey();

  private String apiNameProperty = DEFAULT_API_NAME_PROPERTY;
  private String apiVersionProperty = DEFAULT_API_VERSION_PROPERTY;
  private String otelServiceNameProperty = DEFAULT_OTEL_SERVICE_NAME_PROPERTY;
}

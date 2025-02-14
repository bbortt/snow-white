package io.github.bbortt.snow.white.toolkit.spring.web.config;

import static io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties.PREFIX;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class SpringWebInterceptorProperties {

  static final String PREFIX = "io.github.bbortt.snow.white.toolkit.spring.web";

  private static final String DEFAULT_API_NAME_PROPERTY = "api.name";
  private static final String DEFAULT_API_VERSION_PROPERTY = "api.version";

  /**
   * Name of the attribute correlating an OTEL span with the name of the OpenAPI it presents.
   */
  private String apiNameProperty = DEFAULT_API_NAME_PROPERTY;

  /**
   * Name of the attribute correlating an OTEL span with the version of the OpenAPI it presents.
   */
  private String apiVersionProperty = DEFAULT_API_VERSION_PROPERTY;

  /**
   * Name of the attribute correlating an OTEL span with the name of the service implementing the OpenAPI.
   */
  private @Nullable String otelServiceNameProperty;
}

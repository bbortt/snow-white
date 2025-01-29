package io.github.bbortt.snow.white.toolkit.spring.web.config;

import static io.github.bbortt.snow.white.toolkit.spring.web.config.SpringWebInterceptorProperties.PREFIX;
import static io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class SpringWebInterceptorProperties {

  static final String PREFIX = "io.github.bbortt.snow.white.toolkit.spring.web";

  private static final String DEFAULT_API_NAME_PROPERTY = "api.name";
  private static final String DEFAULT_API_VERSION_PROPERTY = "api.version";
  private static final String DEFAULT_OTEL_SERVICE_NAME_PROPERTY =
    SERVICE_NAME.getKey();

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
  private String otelServiceNameProperty = DEFAULT_OTEL_SERVICE_NAME_PROPERTY;
}

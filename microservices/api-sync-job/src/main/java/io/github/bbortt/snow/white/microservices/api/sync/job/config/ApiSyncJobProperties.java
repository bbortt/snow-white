package io.github.bbortt.snow.white.microservices.api.sync.job.config;

import static io.github.bbortt.snow.white.microservices.api.sync.job.config.ApiSyncJobProperties.PREFIX;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode.GRACEFUL;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi.OpenApiProperties.OAS_INFO_TITLE;
import static io.github.bbortt.snow.white.microservices.api.sync.job.parser.openapi.OpenApiProperties.OAS_INFO_VERSION;
import static java.lang.String.format;
import static org.springframework.util.StringUtils.hasText;

import io.github.bbortt.snow.white.microservices.api.sync.job.parser.ParsingMode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class ApiSyncJobProperties {

  @VisibleForTesting
  static final String PREFIX = "io.github.bbortt.snow.white.sync.job";

  private final ServiceInterface serviceInterface = new ServiceInterface();

  @EventListener({ ApplicationStartedEvent.class })
  public void sanitizeProperties() {
    if (
      !hasText(serviceInterface.baseUrl) || !hasText(serviceInterface.indexUri)
    ) {
      var sirPrefix = PREFIX + ".service-interface";
      throw new IllegalArgumentException(
        format(
          "Both '%s.base-url' and '%s.index-uri' must be set!",
          sirPrefix,
          sirPrefix
        )
      );
    }
  }

  @Getter
  @Setter
  public static class ServiceInterface {

    private static final String DEFAULT_OTEL_SERVICE_NAME_PROPERTY =
      "oas.info.x-service-name";

    private String baseUrl;
    private String indexUri;

    private String apiNameProperty;
    private String apiVersionProperty;
    private String serviceNameProperty = DEFAULT_OTEL_SERVICE_NAME_PROPERTY;

    private ParsingMode parsingMode = GRACEFUL;
  }
}

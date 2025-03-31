package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.api.gateway.config.ApiGatewayProperties.PREFIX;
import static java.util.Objects.nonNull;

import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class ApiGatewayProperties
  implements InitializingBean, EnvironmentAware {

  public static final String PREFIX =
    "io.github.bbortt.snow.white.microservices.api.gateway";

  private @Nullable Environment environment;

  private String publicUrl;
  private String qualityGateApiUrl;
  private String reportCoordinationServiceUrl;

  @Override
  public void afterPropertiesSet() {
    Map<String, String> fields = new HashMap<>();
    fields.put(PREFIX + ".quality-gate-api-url", qualityGateApiUrl);
    fields.put(
      PREFIX + ".report-coordination-service-url",
      reportCoordinationServiceUrl
    );

    if (
      nonNull(environment) && environment.acceptsProfiles(Profiles.of("prod"))
    ) {
      fields.put(PREFIX + ".public-url", publicUrl);
    }

    assertRequiredProperties(fields);
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}

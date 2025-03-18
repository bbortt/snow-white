package io.github.bbortt.snow.white.microservices.api.gateway.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.api.gateway.config.ApiGatewayProperties.PREFIX;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.StringUtils.hasText;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class ApiGatewayProperties implements InitializingBean {

  public static final String PREFIX =
    "io.github.bbortt.snow.white.microservices.api.gateway";

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

    assertRequiredProperties(fields);
  }
}

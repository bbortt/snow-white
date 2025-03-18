package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static io.github.bbortt.snow.white.commons.PropertyUtils.assertRequiredProperties;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.PREFIX;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class ReportCoordinationServiceProperties implements InitializingBean {

  public static final String PREFIX =
    "io.github.bbortt.snow.white.microservices.report.coordination.service";

  private String calculationRequestTopic;

  private String openapiCalculationResponseTopic;

  private String qualityGateApiUrl;

  @Override
  public void afterPropertiesSet() {
    Map<String, String> fields = new HashMap<>();
    fields.put(PREFIX + ".calculation-request-topic", calculationRequestTopic);
    fields.put(
      PREFIX + ".openapi-calculation-response-topic",
      openapiCalculationResponseTopic
    );
    fields.put(PREFIX + ".quality-gate-api-url", qualityGateApiUrl);

    assertRequiredProperties(fields);
  }
}

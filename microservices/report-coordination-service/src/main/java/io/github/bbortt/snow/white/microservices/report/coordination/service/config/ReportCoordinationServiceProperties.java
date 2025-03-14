package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.config.ReportCoordinationServiceProperties.PREFIX;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(PREFIX)
public class ReportCoordinationServiceProperties {

  public static final String PREFIX =
    "io.github.bbortt.snow.white.microservices.report.coordination.service";

  private String calculationRequestTopic;
  private String calculationResponseTopic;
}

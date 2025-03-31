package io.github.bbortt.snow.white.microservices.report.coordination.service.config;

import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.ApiClient;
import io.github.bbortt.snow.white.microservices.report.coordination.service.api.client.qualitygateapi.api.QualityGateApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class QualityGateApiConfig {

  private final String qualityGateApiUrl;

  public QualityGateApiConfig(
    ReportCoordinationServiceProperties reportCoordinationServiceProperties
  ) {
    this.qualityGateApiUrl =
      reportCoordinationServiceProperties.getQualityGateApiUrl();
  }

  @Bean
  public QualityGateApi qualityGateApi(RestClient.Builder builder) {
    var apiClient = new ApiClient(builder.build());
    apiClient.setBasePath(qualityGateApiUrl);
    return new QualityGateApi(apiClient);
  }
}

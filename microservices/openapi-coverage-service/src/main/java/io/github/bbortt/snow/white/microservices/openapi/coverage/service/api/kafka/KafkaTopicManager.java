package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties.PREFIX;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(
  prefix = PREFIX,
  value = "init-topics",
  havingValue = "true"
)
@RequiredArgsConstructor
public class KafkaTopicManager {

  private final OpenApiCoverageServiceProperties openApiCoverageServiceProperties;

  @Bean
  public NewTopic calculationRequestTopic() {
    return TopicBuilder.name(
      openApiCoverageServiceProperties.getCalculationRequestTopic()
    ).build();
  }

  @Bean
  public NewTopic openapiCalculationResponseTopic() {
    return TopicBuilder.name(
      openApiCoverageServiceProperties.getOpenapiCalculationResponseTopic()
    ).build();
  }
}

package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka;

import static io.github.bbortt.snow.white.microservices.openapi.coverage.service.AbstractOpenApiCoverageServiceIT.ADMIN_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.AbstractOpenApiCoverageServiceIT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@Isolated
@DirtiesContext
@SpringBootTest(
  properties = {
    "influxdb.org=snow-white",
    "influxdb.bucket=raw-data",
    "influxdb.token=" + ADMIN_TOKEN,
    "io.github.bbortt.snow.white.microservices.openapi.coverage.service.init-topics=true",
    "io.github.bbortt.snow.white.microservices.openapi.coverage.service.calculation-request-topic=snow-white-coverage-request",
    "io.github.bbortt.snow.white.microservices.openapi.coverage.service.openapi-calculation-response-topic=snow-white-openapi-calculation-response",
  }
)
class KafkaTopicManagerEnabledIT extends AbstractOpenApiCoverageServiceIT {

  @Autowired
  private KafkaTopicManager kafkaTopicManager;

  @Test
  void isEnabledUsingProperties() {
    assertThat(kafkaTopicManager).isNotNull();
  }
}

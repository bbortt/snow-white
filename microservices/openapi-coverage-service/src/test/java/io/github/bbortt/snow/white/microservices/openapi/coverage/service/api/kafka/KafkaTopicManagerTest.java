package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.config.OpenApiCoverageServiceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KafkaTopicManagerTest {

  public static final String TEST_RESPONSE_TOPIC =
    "KafkaTopicManagerTest:response";
  public static final String TEST_REQUEST_TOPIC =
    "KafkaTopicManagerTest:request";

  private KafkaTopicManager fixture;

  @BeforeEach
  void beforeEachSetup() {
    var openApiCoverageServiceProperties =
      new OpenApiCoverageServiceProperties();
    openApiCoverageServiceProperties.setCalculationRequestTopic(
      TEST_REQUEST_TOPIC
    );
    openApiCoverageServiceProperties.setOpenapiCalculationResponseTopic(
      TEST_RESPONSE_TOPIC
    );

    fixture = new KafkaTopicManager(openApiCoverageServiceProperties);
  }

  @Test
  void testInboundTopicCreation() {
    var inboundTopic = fixture.calculationRequestTopic();

    assertThat(inboundTopic.name()).isEqualTo(TEST_REQUEST_TOPIC);
  }

  @Test
  void testOutboundTopicCreation() {
    var outboundTopic = fixture.openapiCalculationResponseTopic();

    assertThat(outboundTopic.name()).isEqualTo(TEST_RESPONSE_TOPIC);
  }
}

package io.github.bbortt.snow.white.microservices.kafka.event.filter.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.kafka.event.filter.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@Isolated
@DirtiesContext
@IntegrationTest
@SpringBootTest(
  properties = "io.github.bbortt.snow.white.kafka.event.filter.init-topics=true"
)
class KafkaTopicManagerEnabledIT {

  @Autowired
  private KafkaTopicManager kafkaTopicManager;

  @Test
  void isEnabledUsingProperties() {
    assertThat(kafkaTopicManager).isNotNull();
  }
}

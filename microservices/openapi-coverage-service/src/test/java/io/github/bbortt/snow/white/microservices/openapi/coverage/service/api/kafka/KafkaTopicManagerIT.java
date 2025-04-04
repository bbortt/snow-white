package io.github.bbortt.snow.white.microservices.openapi.coverage.service.api.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.AbstractOpenApiCoverageServiceIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class KafkaTopicManagerIT extends AbstractOpenApiCoverageServiceIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void isNotInitializedPerDefault() {
    assertThatThrownBy(() -> applicationContext.getBean(KafkaTopicManager.class)
    ).isInstanceOf(NoSuchBeanDefinitionException.class);
  }
}

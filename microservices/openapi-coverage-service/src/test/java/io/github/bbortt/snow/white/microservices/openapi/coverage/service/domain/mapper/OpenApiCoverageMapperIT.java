package io.github.bbortt.snow.white.microservices.openapi.coverage.service.domain.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.AbstractOpenApiCoverageServiceIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

class OpenApiCoverageMapperIT extends AbstractOpenApiCoverageServiceIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void isRegisteredWithinSpringComponentModel() {
    assertThat(
      applicationContext.getBean(OpenApiCoverageMapper.class)
    ).isNotNull();
  }
}

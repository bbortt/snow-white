package io.github.bbortt.snow.white.microservices.openapi.coverage.service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;

class InfluxDBPropertiesTest {

  private InfluxDBProperties fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new InfluxDBProperties();
  }

  @Test
  void isInitializingBean() {
    assertThat(fixture).isInstanceOf(InitializingBean.class);
  }
}

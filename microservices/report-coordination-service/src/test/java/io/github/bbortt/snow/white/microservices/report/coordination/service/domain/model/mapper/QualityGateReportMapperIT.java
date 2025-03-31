package io.github.bbortt.snow.white.microservices.report.coordination.service.domain.model.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

@IntegrationTest
class QualityGateReportMapperIT {

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void isRegisteredWithinSpringComponentModel() {
    assertThat(applicationContext.getBean(ReportMapper.class)).isNotNull();
  }
}

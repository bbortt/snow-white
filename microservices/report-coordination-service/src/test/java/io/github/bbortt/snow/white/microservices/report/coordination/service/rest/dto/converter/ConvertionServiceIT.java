package io.github.bbortt.snow.white.microservices.report.coordination.service.rest.dto.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.report.coordination.service.IntegrationTest;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.Report;
import io.github.bbortt.snow.white.microservices.report.coordination.service.domain.ReportParameters;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

@IntegrationTest
class ConvertionServiceIT {

  @Autowired
  private ConversionService conversionService;

  @Test
  void containsReportConverter() {
    assertThat(
      conversionService.canConvert(Report.class, ReportParameters.class)
    ).isTrue();
  }
}

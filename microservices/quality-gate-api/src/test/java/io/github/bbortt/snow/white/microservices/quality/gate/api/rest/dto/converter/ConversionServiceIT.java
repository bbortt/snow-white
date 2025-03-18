package io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.bbortt.snow.white.microservices.quality.gate.api.IntegrationTest;
import io.github.bbortt.snow.white.microservices.quality.gate.api.domain.QualityGateConfiguration;
import io.github.bbortt.snow.white.microservices.quality.gate.api.rest.dto.QualityGateConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;

@IntegrationTest
class ConversionServiceIT {

  @Autowired
  private ConversionService conversionService;

  @Test
  void containsQualityGateConfigConverter() {
    assertThat(
      conversionService.canConvert(
        QualityGateConfig.class,
        QualityGateConfiguration.class
      )
    ).isTrue();
  }

  @Test
  void containsQualityGateConfigurationConverter() {
    assertThat(
      conversionService.canConvert(
        QualityGateConfiguration.class,
        QualityGateConfig.class
      )
    ).isTrue();
  }
}

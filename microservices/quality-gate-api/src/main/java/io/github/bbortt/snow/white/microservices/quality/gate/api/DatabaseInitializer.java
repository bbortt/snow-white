package io.github.bbortt.snow.white.microservices.quality.gate.api;

import io.github.bbortt.snow.white.microservices.quality.gate.api.service.OpenApiCoverageConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

  private final OpenApiCoverageConfigurationService openApiCoverageConfigurationService;

  @Override
  public void run(String... args) {
    openApiCoverageConfigurationService.initOpenApiCriteria();
  }
}

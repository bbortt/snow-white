package io.github.bbortt.snow.white.microservices.openapi.coverage.service;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.domain.OpenApiCoverage;
import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Main implements CommandLineRunner {

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }

  private final OpenApiCoverageService openApiCoverageService;

  public Main(OpenApiCoverageService openApiCoverageService) {
    this.openApiCoverageService = openApiCoverageService;
  }

  @Override
  public void run(String... args) {
    var openApiCoverage = openApiCoverageService.gatherDataAndCalculateCoverage(
      "example-application",
      "ping-pong",
      "1.0.0"
    );

    logger.info("Coverage: {}", openApiCoverage);
  }
}

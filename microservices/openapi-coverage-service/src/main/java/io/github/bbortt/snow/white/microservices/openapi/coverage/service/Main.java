package io.github.bbortt.snow.white.microservices.openapi.coverage.service;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.OpenApiCoverageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
    openApiCoverageService.calculateTelemetry(
      "example-application",
      "ping-pong",
      "1.0.0"
    );
  }
}

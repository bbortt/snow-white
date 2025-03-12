package io.github.bbortt.snow.white.microservices.openapi.coverage.service;

import io.github.bbortt.snow.white.microservices.openapi.coverage.service.service.TelemetryAnalysisService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Main implements CommandLineRunner {

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }

  private final TelemetryAnalysisService telemetryAnalysisService;

  public Main(TelemetryAnalysisService telemetryAnalysisService) {
    this.telemetryAnalysisService = telemetryAnalysisService;
  }

  @Override
  public void run(String... args) throws Exception {
    telemetryAnalysisService.calculateTelemetry(
      "sample-application",
      "ping-pong",
      "1.0.0"
    );
  }
}

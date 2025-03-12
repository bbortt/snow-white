package io.github.bbortt.snow.white.microservices.openapi.coverage.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Main {

  public static void main(String[] args) {
    SpringApplication.run(Main.class, args);
  }
}

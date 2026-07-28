/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import io.github.bbortt.snow.white.microservices.report.coordinator.api.service.exception.TestResultForUnknownApiException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaListenerErrorHandlingConfig {

  private final ReportCoordinationServiceProperties properties;

  @Bean
  public DefaultErrorHandler defaultErrorHandler() {
    var maxRetries = Math.max(
      Optional.ofNullable(
        properties.getOpenapiCalculationResponse().getMaxRetries()
      ).orElse(2),
      0
    );

    var errorHandler = new DefaultErrorHandler(
      new FixedBackOff(
        Optional.ofNullable(
          properties.getOpenapiCalculationResponse().getBackOffRetryMs()
        ).orElse(5_000L),
        maxRetries
      )
    );

    // Do not retry poison messages that can never be mapped to a known report.
    errorHandler.addNotRetryableExceptions(
      TestResultForUnknownApiException.class
    );

    return errorHandler;
  }
}

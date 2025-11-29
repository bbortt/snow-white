/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@RequiredArgsConstructor
public class KafkaAdminConfig {

  private final KafkaProperties kafkaProperties;

  @Bean
  public KafkaAdmin admin() {
    return new KafkaAdmin(
      Map.of(BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers())
    );
  }
}

/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordinator.api.config;

import static java.util.Collections.singletonList;
import static org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.kafka.core.KafkaAdmin;

@ExtendWith({ MockitoExtension.class })
class KafkaAdminConfigTest {

  @Mock
  private KafkaProperties kafkaPropertiesMock;

  private KafkaAdminConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    fixture = new KafkaAdminConfig(kafkaPropertiesMock);
  }

  @Nested
  class Admin {

    @Test
    void shouldBeInitializedWithBootstrapServers() {
      var bootstrapServers = singletonList("bootstrapServer");
      doReturn(bootstrapServers)
        .when(kafkaPropertiesMock)
        .getBootstrapServers();

      KafkaAdmin kafkaAdmin = fixture.admin();

      assertThat(kafkaAdmin.getConfigurationProperties()).containsExactly(
        Map.entry(BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
      );
    }
  }
}

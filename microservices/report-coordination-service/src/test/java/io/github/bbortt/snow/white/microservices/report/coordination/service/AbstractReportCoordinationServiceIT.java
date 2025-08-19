/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.report.coordination.service;

import static io.github.bbortt.snow.white.microservices.report.coordination.service.AbstractReportCoordinationServiceIT.CALCULATION_REQUEST_TOPIC;
import static io.github.bbortt.snow.white.microservices.report.coordination.service.AbstractReportCoordinationServiceIT.OPENAPI_CALCULATION_RESPONSE_TOPIC;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@ActiveProfiles("test")
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.report.coordination.service.calculation-request-topic=" +
    CALCULATION_REQUEST_TOPIC,
    "snow.white.report.coordination.service.init-topics=true",
    "snow.white.report.coordination.service.openapi-calculation-response.topic=" +
    OPENAPI_CALCULATION_RESPONSE_TOPIC,
    "snow.white.report.coordination.service.public-api-gateway-url=http://localhost:9080",
  }
)
public abstract class AbstractReportCoordinationServiceIT {

  static final String CALCULATION_REQUEST_TOPIC = "snow-white-coverage-request";
  static final String OPENAPI_CALCULATION_RESPONSE_TOPIC =
    "snow-white-openapi-calculation-response";

  protected static final ConfluentKafkaContainer KAFKA_CONTAINER =
    new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.2").withExposedPorts(
      9092
    );

  static final PostgreSQLContainer<?> POSTGRESQL_CONTAINER =
    (PostgreSQLContainer<?>) new PostgreSQLContainer(
      "postgres:17.6-alpine"
    ).withExposedPorts(5432);

  static final WireMockContainer WIRE_MOCK_CONTAINER = new WireMockContainer(
    "wiremock/wiremock:3x-alpine"
  );

  static {
    KAFKA_CONTAINER.start();
    POSTGRESQL_CONTAINER.start();
    WIRE_MOCK_CONTAINER.start();

    WireMock.configureFor(WIRE_MOCK_CONTAINER.getPort());
  }

  @DynamicPropertySource
  static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "spring.kafka.bootstrap-servers",
      KAFKA_CONTAINER::getBootstrapServers
    );
  }

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRESQL_CONTAINER::getJdbcUrl);
    registry.add(
      "spring.datasource.username",
      POSTGRESQL_CONTAINER::getUsername
    );
    registry.add(
      "spring.datasource.password",
      POSTGRESQL_CONTAINER::getPassword
    );
  }

  @DynamicPropertySource
  static void wireMockProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "snow.white.report.coordination.service.quality-gate-api-url",
      WIRE_MOCK_CONTAINER::getBaseUrl
    );
  }
}

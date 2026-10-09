/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config.health;

import static java.lang.String.format;
import static java.time.Duration.ofMinutes;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.kafka.streams.KafkaStreams.State.RUNNING;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.Main;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Boots the service against a real broker and reaches its actuator surface over HTTP.
 * <p>
 * Every subclass gets its own context: a running stream is shared state, and a test that stops
 * one cannot be undone within the context that owns it. Splitting the verdicts across classes
 * keeps each test independent of execution order.
 */
@Isolated
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(
  classes = { Main.class },
  properties = {
    // a random management port keeps the fixed production port (8090) out of the test run
    "management.server.port=0",
    "snow.white.openapi.coverage.stream.api-index.base-url=http://localhost:8085",
    "snow.white.openapi.coverage.stream.calculation-request-topic=snow-white-calculation-request",
    "snow.white.openapi.coverage.stream.init-topics=true",
    "snow.white.openapi.coverage.stream.openapi-calculation-response-topic=snow-white-openapi-calculation-response",
    "tempo.token=a-tempo-token",
    "tempo.url=http://localhost:3200",
  },
  webEnvironment = RANDOM_PORT
)
abstract class AbstractKafkaStreamsHealthIT {

  static final String HEALTH_PATH = "/actuator/health";
  static final String LIVENESS_PATH = HEALTH_PATH + "/liveness";
  static final String READINESS_PATH = HEALTH_PATH + "/readiness";

  static final String INDICATOR_NAME = "kafkaStreams";

  private static final ConfluentKafkaContainer KAFKA_CONTAINER =
    new ConfluentKafkaContainer("confluentinc/cp-kafka:8.3.2").withExposedPorts(
      9092
    );

  static {
    KAFKA_CONTAINER.start();
  }

  @DynamicPropertySource
  static void kafkaProperties(DynamicPropertyRegistry registry) {
    registry.add(
      "spring.kafka.bootstrap-servers",
      KAFKA_CONTAINER::getBootstrapServers
    );
  }

  @Value("${local.server.port}")
  private int localServerPort;

  @Value("${local.management.port}")
  private int localManagementPort;

  @Autowired
  private StreamsBuilderFactoryBean streamsBuilderFactoryBean;

  @Autowired
  private JsonMapper jsonMapper;

  /**
   * Blocks until the topology has joined the group and is processing, so a verdict read
   * afterwards describes a started stream rather than a starting one.
   */
  void awaitStreamRunning() {
    await()
      .atMost(ofMinutes(2))
      .until(() -> {
        var kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

        return nonNull(kafkaStreams) && RUNNING.equals(kafkaStreams.state());
      });
  }

  void closeStream() {
    requireNonNull(streamsBuilderFactoryBean.getKafkaStreams()).close();
  }

  String managementBaseUrl() {
    return baseUrl(localManagementPort);
  }

  String applicationBaseUrl() {
    return baseUrl(localServerPort);
  }

  private String baseUrl(int port) {
    return format("http://localhost:%d", port);
  }

  JsonNode readJson(String baseUrl, String path) {
    var body = RestClient.create(baseUrl)
      .get()
      .uri(path)
      .retrieve()
      .onStatus(HttpStatusCode::isError, (_, _) -> {})
      .body(String.class);

    return jsonMapper.readTree(requireNonNull(body));
  }

  HttpStatusCode statusOf(String baseUrl, String path) {
    return RestClient.create(baseUrl)
      .get()
      .uri(path)
      .retrieve()
      .onStatus(HttpStatusCode::isError, (_, _) -> {})
      .toBodilessEntity()
      .getStatusCode();
  }
}

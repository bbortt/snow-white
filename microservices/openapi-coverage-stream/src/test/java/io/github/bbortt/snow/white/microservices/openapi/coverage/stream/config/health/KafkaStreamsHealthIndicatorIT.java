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
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import clew.traceables.clew.ArchTraceables;
import clew.traceables.clew.SwTraceables;
import clew.traceables.clew.annotation.VerifiesArch;
import clew.traceables.clew.annotation.VerifiesSw;
import io.github.bbortt.snow.white.microservices.openapi.coverage.stream.Main;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
 * Proves the health verdict is reachable from outside the process: served on the management port,
 * carried by both probe groups, and read from the live stream rather than a startup snapshot.
 * <p>
 * The verdict for each individual {@code KafkaStreams.State} is covered exhaustively by
 * {@code KafkaStreamsHealthIndicatorUnitTest}; this test is about the wiring.
 */
@Isolated
@DirtiesContext
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
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
class KafkaStreamsHealthIndicatorIT {

  private static final String HEALTH_PATH = "/actuator/health";
  private static final String LIVENESS_PATH = HEALTH_PATH + "/liveness";
  private static final String READINESS_PATH = HEALTH_PATH + "/readiness";

  private static final String INDICATOR_NAME = "kafkaStreams";

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

  @Test
  @Order(1)
  @VerifiesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
  @VerifiesArch(
    ArchTraceables.ARCH_018_MANAGEMENT_SURFACE_WITHOUT_A_BUSINESS_ONE
  )
  void shouldReportUpAndNameTheState_onceTheTopologyIsRunning() {
    awaitStreamRunning();

    var health = readJson(managementBaseUrl(), HEALTH_PATH);

    assertThat(health.path("status").asString()).isEqualTo("UP");
    assertThat(
      health.path("components").path(INDICATOR_NAME).path("status").asString()
    ).isEqualTo("UP");
    assertThat(
      health
        .path("components")
        .path(INDICATOR_NAME)
        .path("details")
        .path("state")
        .asString()
    ).isEqualTo(RUNNING.name());
  }

  @Test
  @Order(2)
  @VerifiesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
  void shouldReportUpOnBothProbeGroups_onceTheTopologyIsRunning() {
    awaitStreamRunning();

    assertThat(
      readJson(managementBaseUrl(), LIVENESS_PATH).path("status").asString()
    ).isEqualTo("UP");
    assertThat(
      readJson(managementBaseUrl(), READINESS_PATH).path("status").asString()
    ).isEqualTo("UP");
  }

  @Test
  @Order(3)
  @VerifiesArch(
    ArchTraceables.ARCH_018_MANAGEMENT_SURFACE_WITHOUT_A_BUSINESS_ONE
  )
  void shouldServeNothingOnTheApplicationPort() {
    assertThat(statusOf(applicationBaseUrl(), HEALTH_PATH)).isEqualTo(
      HttpStatusCode.valueOf(404)
    );
    assertThat(statusOf(applicationBaseUrl(), "/")).isEqualTo(
      HttpStatusCode.valueOf(404)
    );
  }

  /**
   * Runs last, and dirties the context: closing the stream is not undone.
   */
  @Test
  @Order(4)
  @VerifiesSw(SwTraceables.SW_045_STREAM_STATE_DECIDES_THE_HEALTH_VERDICT)
  void shouldReportDownOnBothProbeGroups_afterTheStreamIsClosed() {
    awaitStreamRunning();

    requireNonNull(streamsBuilderFactoryBean.getKafkaStreams()).close();

    assertThat(statusOf(managementBaseUrl(), HEALTH_PATH)).isEqualTo(
      HttpStatusCode.valueOf(503)
    );
    assertThat(statusOf(managementBaseUrl(), LIVENESS_PATH)).isEqualTo(
      HttpStatusCode.valueOf(503)
    );
    assertThat(statusOf(managementBaseUrl(), READINESS_PATH)).isEqualTo(
      HttpStatusCode.valueOf(503)
    );
  }

  private void awaitStreamRunning() {
    await()
      .atMost(ofMinutes(2))
      .until(() -> {
        var kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

        return nonNull(kafkaStreams) && RUNNING.equals(kafkaStreams.state());
      });
  }

  private String managementBaseUrl() {
    return baseUrl(localManagementPort);
  }

  private String applicationBaseUrl() {
    return baseUrl(localServerPort);
  }

  private String baseUrl(int port) {
    return format("http://localhost:%d", port);
  }

  private JsonNode readJson(String baseUrl, String path) {
    var body = RestClient.create(baseUrl)
      .get()
      .uri(path)
      .retrieve()
      .onStatus(HttpStatusCode::isError, (_, _) -> {})
      .body(String.class);

    return jsonMapper.readTree(requireNonNull(body));
  }

  private HttpStatusCode statusOf(String baseUrl, String path) {
    return RestClient.create(baseUrl)
      .get()
      .uri(path)
      .retrieve()
      .onStatus(HttpStatusCode::isError, (_, _) -> {})
      .toBodilessEntity()
      .getStatusCode();
  }
}

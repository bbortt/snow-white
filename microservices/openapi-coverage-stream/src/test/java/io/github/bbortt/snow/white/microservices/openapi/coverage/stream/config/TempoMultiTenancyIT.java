/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.openapi.coverage.stream.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Reproduces the production incident directly: a real Tempo instance running with
 * {@code multitenancy_enabled: true} rejects every request that doesn't carry an
 * {@code X-Scope-OrgID} header with {@code 401 Unauthorized: "no org id"} - regardless of
 * whether the request is otherwise correctly authenticated.
 *
 * @see <a href="https://grafana.com/docs/tempo/latest/operations/manage-advanced-systems/multitenancy/">Tempo multi-tenancy</a>
 */
class TempoMultiTenancyIT {

  private static final int TEMPO_HTTP_PORT = 3200;

  private static final GenericContainer<?> TEMPO_CONTAINER =
    new GenericContainer<>(DockerImageName.parse("grafana/tempo:2.6.1"))
      .withCopyFileToContainer(
        MountableFile.forClasspathResource("tempo/tempo-multitenant.yaml"),
        "/etc/tempo.yaml"
      )
      .withCommand("-config.file=/etc/tempo.yaml")
      .withExposedPorts(TEMPO_HTTP_PORT)
      .waitingFor(Wait.forLogMessage(".*Tempo started.*\\n", 1));

  static {
    TEMPO_CONTAINER.start();
  }

  private TempoProperties tempoProperties;

  private TempoRestClientConfig fixture;

  @BeforeEach
  void beforeEachSetup() {
    tempoProperties = new TempoProperties();
    tempoProperties.setUrl(
      "http://%s:%s".formatted(
        TEMPO_CONTAINER.getHost(),
        TEMPO_CONTAINER.getMappedPort(TEMPO_HTTP_PORT)
      )
    );
    tempoProperties.setToken("test-token");

    fixture = new TempoRestClientConfig();
  }

  private static final String SEARCH_PATH =
    "/api/search?q={q}&start={start}&end={end}&limit={limit}";

  @Test
  void shouldReturnUnauthorized_whenOrgIdNotConfiguredAgainstMultiTenantTempo() {
    var now = Instant.now().getEpochSecond();

    assertThatThrownBy(() ->
      fixture
        .tempoRestClient(tempoProperties)
        .get()
        .uri(SEARCH_PATH, "{}", now - 3600, now, 1)
        .retrieve()
        .toBodilessEntity()
    )
      .isInstanceOf(HttpClientErrorException.Unauthorized.class)
      .hasMessageContaining("no org id");
  }

  @Test
  void shouldSucceed_whenOrgIdConfiguredAgainstMultiTenantTempo() {
    tempoProperties.setOrgId("snow-white");
    var now = Instant.now().getEpochSecond();

    var response = fixture
      .tempoRestClient(tempoProperties)
      .get()
      .uri(SEARCH_PATH, "{}", now - 3600, now, 1)
      .retrieve()
      .toEntity(String.class);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
  }
}

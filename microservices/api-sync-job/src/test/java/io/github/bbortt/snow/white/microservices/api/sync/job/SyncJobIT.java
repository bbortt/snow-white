/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.sync.job;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okForContentType;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static io.github.bbortt.snow.white.microservices.api.sync.job.util.TestUtils.getResourceContent;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

import com.redis.testcontainers.RedisContainer;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@IntegrationTest
@SpringBootTest(
  classes = { Main.class },
  properties = {
    "snow.white.api.sync.job.service-interface.base-url=${wiremock.server.baseUrl}",
    "snow.white.api.sync.job.service-interface.index-uri=/sir/index",
  }
)
class SyncJobIT {

  private static final int REDIS_PORT = 6379;

  @Container
  static final RedisContainer REDIS_CONTAINER = new RedisContainer(
    DockerImageName.parse("redis:7.4.1-alpine")
  ).withExposedPorts(REDIS_PORT);

  @DynamicPropertySource
  static void redisProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
    registry.add("spring.data.redis.port", () ->
      REDIS_CONTAINER.getMappedPort(REDIS_PORT)
    );
  }

  @Value("${wiremock.server.baseUrl}")
  private String wiremockServerBaseUrl;

  @Autowired
  private SyncJob fixture;

  public static Stream<String> indexContentTypes() {
    return Stream.of(APPLICATION_JSON_VALUE, APPLICATION_OCTET_STREAM_VALUE);
  }

  @ParameterizedTest
  @MethodSource("indexContentTypes")
  void shouldSyncCatalog(String contentType) throws IOException {
    var sirResponse = getResourceContent(
      "sir/full-example-response.json"
    ).replace("${wiremock.server.baseUrl}", wiremockServerBaseUrl);

    stubFor(
      get("/sir/index").willReturn(okForContentType(contentType, sirResponse))
    );

    assertDoesNotThrow(() -> fixture.syncCatalog());
  }
}

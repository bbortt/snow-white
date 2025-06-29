/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway.web.filter;

import io.github.bbortt.snow.white.microservices.api.gateway.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@IntegrationTest
@AutoConfigureWebTestClient
class SpaWebFilterIT {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void testFilterForwardsToIndex() {
    webTestClient
      .get()
      .uri("/")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .contentType("text/html;charset=UTF-8")
      .expectBody(String.class)
      .isEqualTo(SpaWebFilterTestController.INDEX_HTML_TEST_CONTENT);
  }

  @Test
  void testFilterDoesNotForwardToIndexForApi() {
    webTestClient.get().uri("/api/test").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("Hello World");
  }

  @Test
  void testFilterDoesNotForwardToIndexForV3ApiDocs() {
    webTestClient
      .mutate()
      .responseTimeout(Duration.ofMillis(10000))
      .build()
      .get()
      .uri("/v3/api-docs")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .contentType(MediaType.APPLICATION_JSON);
  }

  @Test
  void testFilterDoesNotForwardToIndexForDotFile() {
    webTestClient.get().uri("/file.js").exchange().expectStatus().isNotFound();
  }

  @Test
  void getBackendEndpoint() {
    webTestClient
      .get()
      .uri("/test")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .contentType("text/html;charset=UTF-8")
      .expectBody(String.class)
      .isEqualTo(SpaWebFilterTestController.INDEX_HTML_TEST_CONTENT);
  }

  @Test
  void getSwaggerUi() {
    webTestClient
      .get()
      .uri("/swagger-ui/index.html")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .contentType("text/html")
      .expectBody(String.class);
  }

  @Test
  void forwardUnmappedFirstLevelMapping() {
    webTestClient
      .get()
      .uri("/first-level")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .contentType("text/html;charset=UTF-8")
      .expectBody(String.class)
      .isEqualTo(SpaWebFilterTestController.INDEX_HTML_TEST_CONTENT);
  }

  @Test
  void forwardUnmappedSecondLevelMapping() {
    webTestClient
      .get()
      .uri("/first-level/second-level")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .contentType("text/html;charset=UTF-8")
      .expectBody(String.class)
      .isEqualTo(SpaWebFilterTestController.INDEX_HTML_TEST_CONTENT);
  }

  @Test
  void forwardUnmappedThirdLevelMapping() {
    webTestClient
      .get()
      .uri("/first-level/second-level/third-level")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .contentType("text/html;charset=UTF-8")
      .expectBody(String.class)
      .isEqualTo(SpaWebFilterTestController.INDEX_HTML_TEST_CONTENT);
  }

  @Test
  void forwardUnmappedDeepMapping() {
    webTestClient
      .get()
      .uri("/1/2/3/4/5/6/7/8/9/10")
      .exchange()
      .expectStatus()
      .isOk()
      .expectHeader()
      .contentType("text/html;charset=UTF-8")
      .expectBody(String.class)
      .isEqualTo(SpaWebFilterTestController.INDEX_HTML_TEST_CONTENT);
  }

  @Test
  void getUnmappedFirstLevelFile() {
    webTestClient.get().uri("/foo.js").exchange().expectStatus().isNotFound();
  }

  /**
   * This test verifies that any files that aren't permitted by Spring Security will be forbidden.
   * If you want to change this to return isNotFound(), you need to add a request mapping that
   * allows this file in SecurityConfiguration.
   */
  @Test
  void getUnmappedSecondLevelFile() {
    webTestClient.get().uri("/foo/bar.js").exchange().expectStatus().isUnauthorized();
  }

  @Test
  void getUnmappedThirdLevelFile() {
    webTestClient.get().uri("/foo/another/bar.js").exchange().expectStatus().isUnauthorized();
  }
}

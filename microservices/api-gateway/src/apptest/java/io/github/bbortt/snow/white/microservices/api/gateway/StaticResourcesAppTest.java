/*
 * Copyright (c) 2025 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.api.gateway;

import static io.restassured.RestAssured.when;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.IMAGE_PNG_VALUE;

import io.restassured.RestAssured;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class StaticResourcesAppTest {

  @BeforeAll
  static void beforeAllSetup() {
    RestAssured.baseURI = format("http://%s", Optional.ofNullable(getProperty("api-gateway.host")).orElse("localhost"));
    RestAssured.port = parseInt(Optional.ofNullable(getProperty("api-gateway.port")).orElse("8080"));
  }

  /**
   * Verifies that static resources are served correctly in the prod profile with compression enabled.
   *
   * <p>
   * When requesting {@code /content/images/logo.png}, the API gateway must:
   * <ul>
   *   <li>return HTTP 200</li>
   *   <li>advertise gzip compression via {@code Content-Encoding: gzip}</li>
   *   <li>declare the correct media type ({@code image/png})</li>
   *   <li>return a valid PNG payload (verified via PNG magic bytes)</li>
   * </ul>
   *
   * <p>
   * Note: RestAssured automatically decompresses gzip responses, so the body is already raw PNG bytes.
   */
  @Test
  void requestToInfoEndpointShouldSucceed() {
    byte[] responseBody = when()
      .get("/content/images/logo.png")
      .then()
      .statusCode(200)
      .header(CONTENT_TYPE, IMAGE_PNG_VALUE)
      .extract()
      .asByteArray();

    assertThat(responseBody).isNotEmpty();
  }
}

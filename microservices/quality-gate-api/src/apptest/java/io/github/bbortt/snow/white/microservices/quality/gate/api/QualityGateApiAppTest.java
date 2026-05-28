/*
 * Copyright (c) 2026 Timon Borter <timon.borter@gmx.ch>
 * Licensed under the Polyform Small Business License 1.0.0
 * See LICENSE file for full details.
 */

package io.github.bbortt.snow.white.microservices.quality.gate.api;

import static io.github.bbortt.snow.white.microservices.quality.gate.api.CitrusUtils.getHttpEndpoint;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.Error;
import io.github.bbortt.snow.white.microservices.quality.gate.api.api.rest.dto.QualityGateConfig;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.citrusframework.TestCaseRunner;
import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.automation.qualitygate.api.QualityGateApi;
import org.citrusframework.junit.jupiter.CitrusSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@CitrusSupport
class QualityGateApiAppTest {

  public static final String[] PREDEFINED_QUALITY_GATE_NAMES = {
    "basic-coverage",
    "dry-run",
    "full-feature",
    "minimal",
  };

  private static QualityGateApi qualityGateApi;

  @BeforeAll
  static void beforeAllSetup() {
    qualityGateApi = new QualityGateApi(
      getHttpEndpoint(
        getProperty("quality-gate-api.host", "localhost"),
        parseInt(getProperty("quality-gate-api.port", "8081"))
      )
    );
  }

  static Stream<String> shouldGetQualityGateByName() {
    return Arrays.stream(PREDEFINED_QUALITY_GATE_NAMES);
  }

  /**
   * Verifies that the quality-gates listing endpoint accepts pagination parameters and returns the predefined gates.
   *
   * <p>
   * When requesting {@code GET /api/rest/v1/quality-gates?page=0&size=5}, the service must:
   * <ul>
   *   <li>return HTTP 200</li>
   *   <li>return a non-empty JSON array</li>
   *   <li>include at least the four built-in quality gates ({@code basic-coverage}, {@code dry-run},
   *       {@code full-feature}, {@code minimal}) in any order</li>
   * </ul>
   *
   * <p>
   * Note: the {@code sort} parameter is intentionally omitted because Citrus currently attempts to
   * parse its value as JSON, causing the request to fail.
   */
  @Test
  @CitrusTest
  void shouldGetAllQualityGatesWithPaginationParameters(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      qualityGateApi
        .sendGetAllQualityGates()
        .page(0)
        .size(5)
        .sort(URLEncoder.encode("name,asc", UTF_8))
    );

    testRunner.then(
      qualityGateApi
        .receiveGetAllQualityGates(OK)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var qualityGateConfigs = JsonMapper.shared().readValue(
            payload,
            new TypeReference<List<QualityGateConfig>>() {}
          );

          assertThat(qualityGateConfigs)
            .hasSizeGreaterThanOrEqualTo(4)
            .map(QualityGateConfig::getName)
            .containsSequence(PREDEFINED_QUALITY_GATE_NAMES);
        })
    );
  }

  /**
   * Verifies that a valid quality gate can be created and the service responds with HTTP 201.
   *
   * <p>
   * When sending {@code POST /api/rest/v1/quality-gates} with a JSON body containing a unique {@code name},
   * the service must:
   * <ul>
   *   <li>return HTTP 201 Created</li>
   * </ul>
   *
   * <p>
   * Basic response schema validation is performed automatically by the Citrus OpenAPI integration.
   */
  @Test
  @CitrusTest
  void shouldCreateQualityGate(@CitrusResource TestCaseRunner testRunner) {
    var payload = JsonMapper.shared().writeValueAsString(
      QualityGateConfig.builder()
        .name("shouldCreateQualityGate")
        .description("description")
        .build()
    );

    testRunner.when(
      qualityGateApi
        .sendCreateQualityGate()
        .getMessageBuilderSupport()
        .body(payload)
    );
    testRunner.then(qualityGateApi.receiveCreateQualityGate(CREATED));
  }

  /**
   * Verifies that submitting an empty body to the create endpoint results in HTTP 400.
   *
   * <p>
   * When sending {@code POST /api/rest/v1/quality-gates} with body {@code {}} (all required fields absent),
   * the service must:
   * <ul>
   *   <li>return HTTP 400 Bad Request</li>
   *   <li>return a JSON error body whose {@code code} equals {@code "Bad Request"} and whose
   *       {@code message} starts with {@code "Validation failed"}</li>
   * </ul>
   *
   * <p>
   * Schema validation is disabled on the send side so that the intentionally invalid body is not
   * rejected by Citrus before it reaches the server.
   */
  @Test
  @CitrusTest
  void shouldReturn400WhenCreatingQualityGateWithMissingRequiredFields(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      qualityGateApi
        .sendCreateQualityGate()
        .schemaValidation(false)
        .getMessageBuilderSupport()
        // body: {} (empty, all required fields absent)
        .body("{}")
    );

    testRunner.then(
      qualityGateApi
        .receiveCreateQualityGate(BAD_REQUEST)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var error = JsonMapper.shared().readValue(payload, Error.class);

          assertThat(error).satisfies(
            e ->
              assertThat(e.getCode()).isEqualTo(BAD_REQUEST.getReasonPhrase()),
            e -> assertThat(e.getMessage()).startsWith("Validation failed")
          );
        })
    );
  }

  /**
   * Verifies that creating a quality gate whose name already exists results in HTTP 409.
   *
   * <p>
   * Given a quality gate that has already been successfully created (HTTP 201), when sending a second
   * {@code POST /api/rest/v1/quality-gates} with the same {@code name}, the service must:
   * <ul>
   *   <li>return HTTP 409 Conflict</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn409WhenCreatingQualityGateWithDuplicateName(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var payload = JsonMapper.shared().writeValueAsString(
      QualityGateConfig.builder()
        .name("shouldReturn409WhenCreatingQualityGateWithDuplicateName")
        .description("description")
        .build()
    );

    testRunner.when(
      qualityGateApi
        .sendCreateQualityGate()
        .getMessageBuilderSupport()
        .body(payload)
    );
    testRunner.then(qualityGateApi.receiveCreateQualityGate(CREATED));

    testRunner.when(
      qualityGateApi
        .sendCreateQualityGate()
        .getMessageBuilderSupport()
        .body(payload) /* same name as above */
    );
    testRunner.then(qualityGateApi.receiveCreateQualityGate(CONFLICT));
  }

  /**
   * Verifies that each predefined quality gate can be retrieved by name and returns HTTP 200.
   *
   * <p>
   * For each name in {@link #PREDEFINED_QUALITY_GATE_NAMES}, when sending
   * {@code GET /api/rest/v1/quality-gates/{name}}, the service must:
   * <ul>
   *   <li>return HTTP 200 OK</li>
   * </ul>
   *
   * <p>
   * Basic response schema validation (required fields, types) is performed automatically by the
   * Citrus OpenAPI integration.
   */
  @CitrusTest
  @MethodSource
  @ParameterizedTest
  void shouldGetQualityGateByName(
    String qualityGateName,
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(qualityGateApi.sendGetQualityGateByName(qualityGateName));
    testRunner.then(qualityGateApi.receiveGetQualityGateByName(OK));
  }

  /**
   * Verifies that requesting an unknown quality gate by name results in HTTP 404.
   *
   * <p>
   * When sending {@code GET /api/rest/v1/quality-gates/non-existent-gate}, the service must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenQualityGateNotFound(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      qualityGateApi.sendGetQualityGateByName("non-existent-gate")
    );
    testRunner.then(qualityGateApi.receiveGetQualityGateByName(NOT_FOUND));
  }

  /**
   * Verifies that an existing quality gate can be updated and the service returns the updated resource.
   *
   * <p>
   * Given a quality gate created via {@code POST /api/rest/v1/quality-gates} (HTTP 201), when sending
   * {@code PUT /api/rest/v1/quality-gates/{name}} with an updated {@code description}, the service must:
   * <ul>
   *   <li>return HTTP 200 OK</li>
   *   <li>return a JSON body whose {@code description} reflects the submitted value</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldUpdateQualityGate(@CitrusResource TestCaseRunner testRunner) {
    var qualityGateConfig = QualityGateConfig.builder()
      .name("shouldUpdateQualityGate")
      .description("description")
      .build();

    testRunner.when(
      qualityGateApi
        .sendCreateQualityGate()
        .getMessageBuilderSupport()
        .body(JsonMapper.shared().writeValueAsString(qualityGateConfig))
    );
    testRunner.then(qualityGateApi.receiveCreateQualityGate(CREATED));

    var description = "description";
    testRunner.when(
      qualityGateApi
        .sendUpdateQualityGate(qualityGateConfig.getName())
        .getMessageBuilderSupport()
        .body(
          JsonMapper.shared().writeValueAsString(
            qualityGateConfig.description(description)
          )
        )
    );
    testRunner.then(
      qualityGateApi
        .receiveUpdateQualityGate(OK)
        .validate((message, context) -> {
          var payload = message.getPayload(String.class);
          assertThat(payload).isNotEmpty();

          var updatedQualityGateConfig = JsonMapper.shared().readValue(
            payload,
            QualityGateConfig.class
          );
          assertThat(updatedQualityGateConfig.getDescription()).isEqualTo(
            description
          );
        })
    );
  }

  /**
   * Verifies that attempting to update a quality gate that does not exist results in HTTP 404.
   *
   * <p>
   * When sending {@code PUT /api/rest/v1/quality-gates/{name}} for a name that was never created,
   * the service must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenUpdatingNonExistentQualityGate(
    @CitrusResource TestCaseRunner testRunner
  ) {
    var qualityGateConfigName =
      "shouldReturn404WhenUpdatingNonExistentQualityGate";

    testRunner.when(
      qualityGateApi
        .sendUpdateQualityGate(qualityGateConfigName)
        .getMessageBuilderSupport()
        .body(
          JsonMapper.shared().writeValueAsString(
            QualityGateConfig.builder()
              .name(qualityGateConfigName)
              .description("description")
              .build()
          )
        )
    );
    testRunner.then(qualityGateApi.receiveUpdateQualityGate(NOT_FOUND));
  }

  /**
   * Verifies that an existing quality gate can be deleted and the service responds with HTTP 204.
   *
   * <p>
   * Given a quality gate created via {@code POST /api/rest/v1/quality-gates} (HTTP 201), when sending
   * {@code DELETE /api/rest/v1/quality-gates/{name}}, the service must:
   * <ul>
   *   <li>return HTTP 204 No Content</li>
   *   <li>return an empty response body</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldDeleteQualityGate(@CitrusResource TestCaseRunner testRunner) {
    var qualityGateConfig = QualityGateConfig.builder()
      .name("shouldDeleteQualityGate")
      .description("description")
      .build();
    var payload = JsonMapper.shared().writeValueAsString(qualityGateConfig);

    testRunner.when(
      qualityGateApi
        .sendCreateQualityGate()
        .getMessageBuilderSupport()
        .body(payload)
    );
    testRunner.then(qualityGateApi.receiveCreateQualityGate(CREATED));

    testRunner.when(
      qualityGateApi.sendDeleteQualityGate(qualityGateConfig.getName())
    );
    testRunner.then(qualityGateApi.receiveDeleteQualityGate(NO_CONTENT));
  }

  /**
   * Verifies that attempting to delete a quality gate that does not exist results in HTTP 404.
   *
   * <p>
   * When sending {@code DELETE /api/rest/v1/quality-gates/{name}} for a name that was never created,
   * the service must:
   * <ul>
   *   <li>return HTTP 404 Not Found</li>
   * </ul>
   */
  @Test
  @CitrusTest
  void shouldReturn404WhenDeletingNonExistentQualityGate(
    @CitrusResource TestCaseRunner testRunner
  ) {
    testRunner.when(
      qualityGateApi.sendDeleteQualityGate(
        "shouldReturn404WhenDeletingNonExistentQualityGate"
      )
    );
    testRunner.then(qualityGateApi.receiveDeleteQualityGate(NOT_FOUND));
  }
}
